package com.portfolio.eventsourcing.domain;

import java.time.Instant;

public class OrderService {
    private final OrderEventRepository repository;

    public OrderService(OrderEventRepository repository) {
        this.repository = repository;
    }

    public OrderEvent handle(OrderCommand command) {
        OrderEvent event = switch (command) {
            case OrderCommand.CreateOrder c ->
                new OrderEvent.OrderCreated(
                    c.orderId(), c.customerName(), c.product(), c.quantity(), Instant.now());

            case OrderCommand.ShipOrder c -> {
                var events = repository.findByOrderId(c.orderId());
                var order = new Order(events);
                if (!"CREATED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot be shipped (status: " + order.getStatus() + ")");
                }
                yield new OrderEvent.OrderShipped(c.orderId(), c.trackingNumber(), Instant.now());
            }

            case OrderCommand.CancelOrder c -> {
                var events = repository.findByOrderId(c.orderId());
                var order = new Order(events);
                if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot be cancelled (status: " + order.getStatus() + ")");
                }
                yield new OrderEvent.OrderCancelled(c.orderId(), c.reason(), Instant.now());
            }

            case OrderCommand.DeliverOrder c -> {
                var events = repository.findByOrderId(c.orderId());
                var order = new Order(events);
                if (!"SHIPPED".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order " + c.orderId() + " cannot be delivered (status: " + order.getStatus() + ")");
                }
                yield new OrderEvent.OrderDelivered(c.orderId(), Instant.now());
            }
        };

        repository.append(event);
        return event;
    }
}
