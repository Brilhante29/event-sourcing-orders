package com.portfolio.eventsourcing.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.eventsourcing.EventsourcingApplication;
import com.portfolio.eventsourcing.application.CqrsProjection;
import com.portfolio.eventsourcing.domain.OrderCommand;
import com.portfolio.eventsourcing.domain.OrderService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public final class BenchmarkRunner {
    private static final int EVENTS_PER_ORDER = 4;

    private BenchmarkRunner() {}

    public static void main(String[] args) throws Exception {
        int orders = integerEnvironment("BENCHMARK_ORDERS", 250);
        int warmupOrders = integerEnvironment("BENCHMARK_WARMUP_ORDERS", 25);
        int repetitions = integerEnvironment("BENCHMARK_REPETITIONS", 3);
        Instant startedAt = Instant.now();

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EventsourcingApplication.class)
            .web(WebApplicationType.NONE)
            .run(args)) {
            var jdbc = context.getBean(JdbcTemplate.class);
            var service = context.getBean(OrderService.class);
            var projection = context.getBean(CqrsProjection.class);

            reset(jdbc);
            runWorkload(service, warmupOrders, "warmup");
            projection.rebuild();

            var throughputSamples = new ArrayList<Double>();
            var rebuildSamples = new ArrayList<Double>();
            var replaySamples = new ArrayList<Double>();
            int failures = 0;

            for (int repetition = 1; repetition <= repetitions; repetition++) {
                reset(jdbc);
                long appendStarted = System.nanoTime();
                long totalEvents = runWorkload(service, orders, "run-" + repetition);
                double appendSeconds = secondsSince(appendStarted);

                long rebuildStarted = System.nanoTime();
                long replayedEvents = projection.rebuild();
                double rebuildMillis = millisSince(rebuildStarted);

                if (replayedEvents != totalEvents
                    || projection.checkpoint() != totalEvents
                    || projection.getAllOrders().size() != orders) {
                    failures++;
                }
                throughputSamples.add(totalEvents / appendSeconds);
                rebuildSamples.add(rebuildMillis);
                replaySamples.add((double) replayedEvents);
            }

            if (failures > 0) {
                throw new IllegalStateException("Benchmark invariants failed in " + failures + " repetitions");
            }

            var result = createResult(
                orders, warmupOrders, repetitions, throughputSamples, rebuildSamples,
                replaySamples, startedAt, Duration.between(startedAt, Instant.now()));
            writeResult(result);
        }
    }

    static Map<String, Object> createResult(
        int orders,
        int warmupOrders,
        int repetitions,
        List<Double> throughputSamples,
        List<Double> rebuildSamples,
        List<Double> replaySamples,
        Instant startedAt,
        Duration duration
    ) throws Exception {
        String config = "postgres16|orders=" + orders + "|events=" + EVENTS_PER_ORDER
            + "|warmup=" + warmupOrders + "|repeat=" + repetitions;
        String sampleEvidence = throughputSamples + "|" + rebuildSamples + "|" + replaySamples;
        var result = new LinkedHashMap<String, Object>();
        result.put("schema_version", 2);
        result.put("run_id", UUID.randomUUID().toString());
        result.put("project", "event-sourcing-orders");
        result.put("benchmark_id", "postgres-append-rebuild");
        result.put("workload", Map.of(
            "version", "1.0.0",
            "fixture_digest", sha256("deterministic-order-fixture-v1"),
            "config_digest", sha256(config),
            "warmup_iterations", warmupOrders,
            "measured_iterations", orders,
            "concurrency", 1));
        result.put("metrics", List.of(
            metric("append_throughput", median(throughputSamples), "events/s",
                "higher_is_better", throughputSamples, Map.of("statistic", "median")),
            metric("projection_rebuild_latency", median(rebuildSamples), "ms",
                "lower_is_better", rebuildSamples, Map.of("statistic", "median")),
            metric("replayed_events", median(replaySamples), "events",
                "target", replaySamples, Map.of("expected_per_run", orders * EVENTS_PER_ORDER))));
        result.put("execution", Map.of(
            "command", "powershell -NoProfile -ExecutionPolicy Bypass -File tools/benchmark.ps1",
            "started_at", startedAt.toString(),
            "duration_seconds", duration.toNanos() / 1_000_000_000.0,
            "exit_code", 0,
            "repeat", repetitions));
        result.put("environment", Map.of(
            "runtime", "Java " + System.getProperty("java.version") + " / PostgreSQL 16.4",
            "architecture", System.getProperty("os.arch", "unknown"),
            "hardware_class", environment("HARDWARE_CLASS", "local-docker")));
        result.put("provenance", Map.of(
            "source_commit", requiredSha("SOURCE_COMMIT"),
            "clean_tree", Boolean.parseBoolean(environment("CLEAN_TREE", "true")),
            "image_ref", "event-sourcing-orders:benchmark",
            "image_digest", requiredDigest("IMAGE_DIGEST"),
            "dependency_lock_digest", dependencyDigest(),
            "producer", environment("BENCHMARK_PRODUCER", "local"),
            "artifact_digest", sha256(sampleEvidence)));
        result.put("comparability_key", "event-sourcing-orders:postgres16:orders" + orders
            + ":events" + EVENTS_PER_ORDER + ":repeat" + repetitions);
        return result;
    }

    private static Map<String, Object> metric(
        String name,
        double value,
        String unit,
        String direction,
        List<Double> samples,
        Map<String, Object> summary
    ) {
        return Map.of(
            "name", name,
            "value", value,
            "unit", unit,
            "direction", direction,
            "samples", samples,
            "failures", 0,
            "summary", summary);
    }

    private static long runWorkload(OrderService service, int orders, String runId) {
        for (int index = 0; index < orders; index++) {
            UUID orderId = UUID.nameUUIDFromBytes(
                (runId + ":" + index).getBytes(StandardCharsets.UTF_8));
            service.handle(new OrderCommand.CreateOrder(
                orderId, "Customer-" + index, "Product-" + (index % 25),
                (index % 5) + 1, 1000 + index, "BRL"));
            service.handle(new OrderCommand.AuthorizePayment(orderId));
            service.handle(new OrderCommand.ShipOrder(orderId, "TRACK-" + index));
            service.handle(new OrderCommand.DeliverOrder(orderId));
        }
        return (long) orders * EVENTS_PER_ORDER;
    }

    private static void reset(JdbcTemplate jdbc) {
        jdbc.update("TRUNCATE order_events, order_projection, projection_checkpoint RESTART IDENTITY");
    }

    private static void writeResult(Map<String, Object> result) throws Exception {
        Path output = Path.of(environment(
            "BENCHMARK_OUTPUT", "benchmarks/results/event-sourcing-orders-v2.json"));
        Files.createDirectories(output.toAbsolutePath().getParent());
        var mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        Files.writeString(output, json + System.lineSeparator(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    private static double median(List<Double> values) {
        var sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 0
            ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0
            : sorted.get(middle);
    }

    private static double secondsSince(long started) {
        return (System.nanoTime() - started) / 1_000_000_000.0;
    }

    private static double millisSince(long started) {
        return (System.nanoTime() - started) / 1_000_000.0;
    }

    private static int integerEnvironment(String name, int fallback) {
        return Integer.parseInt(environment(name, Integer.toString(fallback)));
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String requiredSha(String name) {
        String value = environment(name, "0000000000000000000000000000000000000000");
        if (!value.matches("^[0-9a-f]{40}$")) {
            throw new IllegalArgumentException(name + " must be a 40-character lowercase Git SHA");
        }
        return value;
    }

    private static String requiredDigest(String name) {
        String value = environment(name, "sha256:" + "0".repeat(64));
        if (!value.matches("^sha256:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(name + " must be a sha256 digest");
        }
        return value;
    }

    private static String dependencyDigest() throws Exception {
        Path catalog = Path.of("gradle/libs.versions.toml");
        return Files.exists(catalog) ? sha256(Files.readString(catalog)) : sha256("catalog-unavailable");
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8));
        return "sha256:" + HexFormat.of().formatHex(digest);
    }
}
