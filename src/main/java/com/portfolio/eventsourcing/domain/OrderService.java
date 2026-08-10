package com.portfolio.eventsourcing.domain;

import com.portfolio.eventsourcing.application.port.out.PaymentAuthorizer;
import java.time.Instant;
import java.util.UUID;

public class OrderService {
    private final OrderEventRepository repository;
    private final PaymentAuthorizer paymentAuthorizer;

    public OrderService(OrderEventRepository repository, PaymentAuthorizer paymentAuthorizer) {
        this.repository = repository;
        this.paymentAuthorizer = paymentAuthorizer;
    }

    public OrderEvent handle(OrderCommand command) {
        PendingEvent pending = switch (command) {
            case OrderCommand.CreateOrder c -> {
                require(c.amountMinor() > 0, "amountMinor must be greater than zero");
                require(c.currency() != null && c.currency().matches("^[A-Za-z]{3}$"),
                    "currency must be a three-letter code");
                if (!repository.findStream(c.orderId()).isEmpty()) {
                    throw new OptimisticConcurrencyException(c.orderId(), 0);
                }
                yield new PendingEvent(new OrderEvent.OrderCreated(
                    c.orderId(), c.customerName(), c.product(), c.quantity(),
                    c.amountMinor(), c.currency().toUpperCase(), Instant.now()), 0);
            }

            case OrderCommand.AuthorizePayment c -> {
                var stream = repository.findStream(c.orderId());
                var order = requireExistingOrder(c.orderId(), stream);
                if (order.getPaymentId() != null) {
                    throw new IllegalStateException("Order " + c.orderId() + " already has an authorized payment");
                }
                if (!"CREATED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot authorize payment (status: " + order.getStatus() + ")");
                }
                var authorization = paymentAuthorizer.authorize(
                    new PaymentAuthorizer.PaymentAuthorizationRequest(
                        c.orderId(), order.getAmountMinor(), order.getCurrency()));
                yield new PendingEvent(new OrderEvent.OrderPaymentAuthorized(
                    c.orderId(), authorization.paymentId(), order.getAmountMinor(),
                    order.getCurrency(), Instant.now()), stream.size());
            }

            case OrderCommand.ShipOrder c -> {
                var stream = repository.findStream(c.orderId());
                var order = requireExistingOrder(c.orderId(), stream);
                if (order.getPaymentId() == null) {
                    throw new IllegalStateException("Order " + c.orderId() + " requires payment authorization");
                }
                if (!"CREATED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot be shipped (status: " + order.getStatus() + ")");
                }
                yield new PendingEvent(
                    new OrderEvent.OrderShipped(c.orderId(), c.trackingNumber(), Instant.now()), stream.size());
            }

            case OrderCommand.CancelOrder c -> {
                var stream = repository.findStream(c.orderId());
                var order = requireExistingOrder(c.orderId(), stream);
                if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot be cancelled (status: " + order.getStatus() + ")");
                }
                yield new PendingEvent(
                    new OrderEvent.OrderCancelled(c.orderId(), c.reason(), Instant.now()), stream.size());
            }

            case OrderCommand.DeliverOrder c -> {
                var stream = repository.findStream(c.orderId());
                var order = requireExistingOrder(c.orderId(), stream);
                if (!"SHIPPED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot be delivered (status: " + order.getStatus() + ")");
                }
                yield new PendingEvent(
                    new OrderEvent.OrderDelivered(c.orderId(), Instant.now()), stream.size());
            }
        };

        repository.append(
            pending.event(), pending.expectedVersion(), pending.event().orderId(), UUID.randomUUID());
        return pending.event();
    }

    private static Order requireExistingOrder(UUID orderId, java.util.List<StoredOrderEvent> stream) {
        if (stream.isEmpty()) {
            throw new IllegalStateException("Order " + orderId + " does not exist");
        }
        return new Order(stream.stream().map(StoredOrderEvent::payload).toList());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record PendingEvent(OrderEvent event, long expectedVersion) {}
}
