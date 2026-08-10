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
        var event = new OrderEvent.OrderCreated(orderId, "Alice", "Widget", 3, 2590, "BRL", Instant.now());
        var order = new Order(List.of(event));
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getCustomerName()).isEqualTo("Alice");
        assertThat(order.getProduct()).isEqualTo("Widget");
        assertThat(order.getQuantity()).isEqualTo(3);
        assertThat(order.getAmountMinor()).isEqualTo(2590);
        assertThat(order.getCurrency()).isEqualTo("BRL");
        assertThat(order.getStatus()).isEqualTo("CREATED");
    }

    @Test
    void shouldRebuildFromMultipleEvents() {
        var orderId = UUID.randomUUID();
        List<OrderEvent> events = List.of(
            new OrderEvent.OrderCreated(orderId, "Bob", "Gadget", 1, 1990, "BRL", Instant.now()),
            new OrderEvent.OrderPaymentAuthorized(orderId, UUID.randomUUID(), 1990, "BRL", Instant.now()),
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
            new OrderEvent.OrderCreated(orderId, "Carol", "Service", 2, 4500, "BRL", Instant.now()),
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
            new OrderEvent.OrderCreated(orderId, "Dave", "Tool", 5, 8700, "BRL", Instant.now()));
        var order = new Order(events);
        order.apply(new OrderEvent.OrderShipped(orderId, "TRACK-456", Instant.now()));
        assertThat(order.getStatus()).isEqualTo("SHIPPED");
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK-456");
    }

    @Test
    void shouldRestorePaymentAuthorizationFromEventHistory() {
        var orderId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var order = new Order(List.of(
            new OrderEvent.OrderCreated(orderId, "Eva", "Plan", 1, 1200, "BRL", Instant.now()),
            new OrderEvent.OrderPaymentAuthorized(orderId, paymentId, 1200, "BRL", Instant.now())));

        assertThat(order.getPaymentId()).isEqualTo(paymentId);
    }
}
