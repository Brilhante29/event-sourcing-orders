package com.portfolio.eventsourcing.application;

import com.portfolio.eventsourcing.domain.Order;
import java.util.UUID;

public record OrderView(
    UUID orderId,
    String customerName,
    String product,
    int quantity,
    long amountMinor,
    String currency,
    String status,
    String trackingNumber,
    String cancellationReason,
    UUID paymentId
) {
    public static OrderView from(Order order) {
        return new OrderView(
            order.getId(), order.getCustomerName(), order.getProduct(), order.getQuantity(),
            order.getAmountMinor(), order.getCurrency(), order.getStatus(),
            order.getTrackingNumber(), order.getCancellationReason(), order.getPaymentId());
    }
}
