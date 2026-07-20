package com.portfolio.eventsourcing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldRebuildFromSingleEvent() {
        var orderId = UUID.randomUUID();
        var event = new OrderEvent.OrderCreated(orderId, "Alice", "Widget", 3, Instant.now());
        var order = new Order(List.of(event));
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getCustomerName()).isEqualTo("Alice");
        assertThat(order.getProduct()).isEqualTo("Widget");
        assertThat(order.getQuantity()).isEqualTo(3);
        assertThat(order.getStatus()).isEqualTo("CREATED");
    }

    @Test
    void shouldRebuildFromMultipleEvents() {
        var orderId = UUID.randomUUID();
        List<OrderEvent> events = List.of(
            new OrderEvent.OrderCreated(orderId, "Bob", "Gadget", 1, Instant.now()),
            new OrderEvent.OrderShipped(orderId, "TRACK-123", Instant.now()),
            new OrderEvent.OrderDelivered(orderId, Instant.now()));
        var order = new Order(events);
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getStatus()).isEqualTo("DELIVERED");
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK-123");
    }

    @Test
    void shouldRebuildWithCancellation() {
        var orderId = UUID.randomUUID();
        List<OrderEvent> events = List.of(
            new OrderEvent.OrderCreated(orderId, "Carol", "Service", 2, Instant.now()),
            new OrderEvent.OrderCancelled(orderId, "out of stock", Instant.now()));
        var order = new Order(events);
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        assertThat(order.getCancellationReason()).isEqualTo("out of stock");
    }

    @Test
    void shouldHandleEmptyEventList() {
        var order = new Order(List.of());
        assertThat(order.getId()).isNull();
        assertThat(order.getStatus()).isNull();
    }

    @Test
    void shouldApplyEventAfterConstruction() {
        var orderId = UUID.randomUUID();
        List<OrderEvent> events = List.of(
            new OrderEvent.OrderCreated(orderId, "Dave", "Tool", 5, Instant.now()));
        var order = new Order(events);
        order.apply(new OrderEvent.OrderShipped(orderId, "TRACK-456", Instant.now()));
        assertThat(order.getStatus()).isEqualTo("SHIPPED");
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK-456");
    }
}
