package com.portfolio.eventsourcing.application;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.portfolio.eventsourcing.domain.OrderEvent;

class EventStoreTest {

    @Test
    void shouldAppendAndRetrieveByOrderId() {
        var store = new EventStore();
        var orderId = UUID.randomUUID();
        var event = new OrderEvent.OrderCreated(orderId, "Test", "Item", 1, Instant.now());
        store.append(event);
        var retrieved = store.findByOrderId(orderId);
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).orderId()).isEqualTo(orderId);
    }

    @Test
    void shouldReturnEmptyForUnknownOrder() {
        var store = new EventStore();
        var retrieved = store.findByOrderId(UUID.randomUUID());
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldFindAllEvents() {
        var store = new EventStore();
        store.append(new OrderEvent.OrderCreated(UUID.randomUUID(), "A", "X", 1, Instant.now()));
        store.append(new OrderEvent.OrderCreated(UUID.randomUUID(), "B", "Y", 2, Instant.now()));
        assertThat(store.findAll()).hasSize(2);
    }

    @Test
    void shouldRetainOrderAcrossMultipleAppends() {
        var store = new EventStore();
        var orderId = UUID.randomUUID();
        store.append(new OrderEvent.OrderCreated(orderId, "C", "Z", 3, Instant.now()));
        store.append(new OrderEvent.OrderShipped(orderId, "TRACK-999", Instant.now()));
        var events = store.findByOrderId(orderId);
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(OrderEvent.OrderCreated.class);
        assertThat(events.get(1)).isInstanceOf(OrderEvent.OrderShipped.class);
    }
}
