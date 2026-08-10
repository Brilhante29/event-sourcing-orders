package com.portfolio.eventsourcing.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkResultTest {
    @Test
    void shouldCreateThreeSampleV2Evidence() throws Exception {
        var result = BenchmarkRunner.createResult(
            10, 2, 3,
            List.of(100.0, 120.0, 110.0),
            List.of(8.0, 7.0, 9.0),
            List.of(40.0, 40.0, 40.0),
            Instant.parse("2026-08-10T00:00:00Z"), Duration.ofSeconds(1));

        assertThat(result).containsEntry("schema_version", 2);
        assertThat(result.get("comparability_key").toString()).contains("repeat3");
        assertThat((List<?>) result.get("metrics")).hasSize(3);
    }
}
