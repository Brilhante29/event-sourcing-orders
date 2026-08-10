package com.portfolio.eventsourcing.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderEvent.OrderCreated.class, name = "OrderCreated"),
    @JsonSubTypes.Type(value = OrderEvent.OrderPaymentAuthorized.class, name = "OrderPaymentAuthorized"),
    @JsonSubTypes.Type(value = OrderEvent.OrderShipped.class, name = "OrderShipped"),
    @JsonSubTypes.Type(value = OrderEvent.OrderCancelled.class, name = "OrderCancelled"),
    @JsonSubTypes.Type(value = OrderEvent.OrderDelivered.class, name = "OrderDelivered")
})
public sealed interface OrderEvent {
    UUID orderId();
    Instant timestamp();

    record OrderCreated(
        UUID orderId,
        String customerName,
        String product,
        int quantity,
        long amountMinor,
        String currency,
        Instant timestamp
    ) implements OrderEvent {}

    record OrderPaymentAuthorized(
        UUID orderId,
        UUID paymentId,
        long amountMinor,
        String currency,
        Instant timestamp
    ) implements OrderEvent {}

    record OrderShipped(
        UUID orderId,
        String trackingNumber,
        Instant timestamp
    ) implements OrderEvent {}

    record OrderCancelled(
        UUID orderId,
        String reason,
        Instant timestamp
    ) implements OrderEvent {}

    record OrderDelivered(
        UUID orderId,
        Instant timestamp
    ) implements OrderEvent {}
}
