package com.portfolio.eventsourcing.domain;

import java.util.UUID;

public sealed interface OrderCommand {
    record CreateOrder(
        UUID orderId,
        String customerName,
        String product,
        int quantity,
        long amountMinor,
        String currency
    ) implements OrderCommand {}

    record AuthorizePayment(UUID orderId) implements OrderCommand {}

    record ShipOrder(
        UUID orderId,
        String trackingNumber
    ) implements OrderCommand {}

    record CancelOrder(
        UUID orderId,
        String reason
    ) implements OrderCommand {}

    record DeliverOrder(
        UUID orderId
    ) implements OrderCommand {}
}
