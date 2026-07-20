package com.portfolio.eventsourcing.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.eventsourcing.application.CqrsProjection;
import com.portfolio.eventsourcing.application.EventStore;
import com.portfolio.eventsourcing.domain.OrderCommand;
import com.portfolio.eventsourcing.domain.OrderService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BenchmarkRunner {

    public static void main(String[] args) throws Exception {
        int numOrders = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
        int eventsPerOrder = args.length > 1 ? Integer.parseInt(args[1]) : 5;

        var eventStore = new EventStore();
        var orderService = new OrderService(eventStore);
        var projection = new CqrsProjection(eventStore);

        System.out.printf("Benchmark: %d orders, %d events per order%n", numOrders, eventsPerOrder);

        long totalEvents = 0;
        var start = Instant.now();

        for (int i = 0; i < numOrders; i++) {
            var orderId = UUID.randomUUID();
            var prefix = "Customer-" + i;
            var product = "Product-" + (i % 100);
            var qty = (i % 10) + 1;

            orderService.handle(new OrderCommand.CreateOrder(orderId, prefix, product, qty));
            totalEvents++;

            if (eventsPerOrder >= 2) {
                orderService.handle(new OrderCommand.ShipOrder(orderId, "TRACK-" + orderId));
                totalEvents++;
            }

            if (eventsPerOrder >= 3) {
                if (i % 3 == 0) {
                    orderService.handle(new OrderCommand.CancelOrder(orderId, "inventory issue"));
                } else {
                    orderService.handle(new OrderCommand.DeliverOrder(orderId));
                }
                totalEvents++;
            }

            if (eventsPerOrder >= 4) {
                var orderId2 = UUID.randomUUID();
                orderService.handle(new OrderCommand.CreateOrder(orderId2, prefix + "-b", product, qty));
                totalEvents++;
                orderService.handle(new OrderCommand.ShipOrder(orderId2, "TRACK-B-" + orderId2));
                totalEvents++;
                orderService.handle(new OrderCommand.DeliverOrder(orderId2));
                totalEvents++;
            }

            if (eventsPerOrder >= 5) {
                var orderId3 = UUID.randomUUID();
                orderService.handle(new OrderCommand.CreateOrder(orderId3, prefix + "-c", product, qty));
                totalEvents++;
                orderService.handle(new OrderCommand.ShipOrder(orderId3, "TRACK-C-" + orderId3));
                totalEvents++;
                orderService.handle(new OrderCommand.DeliverOrder(orderId3));
                totalEvents++;
            }
        }

        var end = Instant.now();
        var duration = Duration.between(start, end);
        double seconds = duration.toNanos() / 1_000_000_000.0;
        double eps = totalEvents / seconds;

        projection.rebuild();

        System.out.printf("Total events: %d%n", totalEvents);
        System.out.printf("Elapsed: %.3f s%n", seconds);
        System.out.printf("Events/s: %.2f%n", eps);
        System.out.printf("Orders in projection: %d%n", projection.getAllOrders().size());

        var result = new LinkedHashMap<String, Object>();
        result.put("project", "event-sourcing-orders");
        result.put("metric", "events_per_second");
        result.put("value", Math.round(eps));
        result.put("unit", "events/s");
        result.put("numOrders", numOrders);
        result.put("eventsPerOrder", eventsPerOrder);
        result.put("totalEvents", totalEvents);
        result.put("durationSeconds", String.format("%.3f", seconds));
        result.put("timestamp", Instant.now().toString());
        result.put("environment", Map.of(
            "java", System.getProperty("java.version") != null ? System.getProperty("java.version") : "unknown",
            "os", System.getProperty("os.name") != null ? System.getProperty("os.name") : "unknown"));

        var mapper = new ObjectMapper();
        var json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println(json);
    }
}
