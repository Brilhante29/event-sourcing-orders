package com.portfolio.eventsourcing.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class BenchmarkRunnerTest {

    @Test
    void benchmarkProducesExpectedJson() throws Exception {
        var out = new ByteArrayOutputStream();
        var oldOut = System.out;
        System.setOut(new PrintStream(out));
        try {
            BenchmarkRunner.main(new String[]{"100", "3"});
        } finally {
            System.setOut(oldOut);
        }
        var output = out.toString();
        assertThat(output).contains("events_per_second");
        assertThat(output).contains("event-sourcing-orders");
        assertThat(output).contains("\"value\"");
        assertThat(output).contains("\"unit\"");
        assertThat(output).contains("\"totalEvents\"");
    }
}
