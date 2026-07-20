package com.portfolio.eventsourcing.domain;

import java.util.List;
import java.util.UUID;

public class Order {
    private UUID id;
    private String customerName;
    private String product;
    private int quantity;
    private String status;
    private String trackingNumber;
    private String cancellationReason;

    public Order() {}

    public Order(List<OrderEvent> events) {
        for (var event : events) {
            apply(event);
        }
    }

    public void apply(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderCreated e -> {
                this.id = e.orderId();
                this.customerName = e.customerName();
                this.product = e.product();
                this.quantity = e.quantity();
                this.status = "CREATED";
            }
            case OrderEvent.OrderShipped e -> {
                this.trackingNumber = e.trackingNumber();
                this.status = "SHIPPED";
            }
            case OrderEvent.OrderCancelled e -> {
                this.cancellationReason = e.reason();
                this.status = "CANCELLED";
            }
            case OrderEvent.OrderDelivered e -> {
                this.status = "DELIVERED";
            }
        }
    }

    public UUID getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCancellationReason() { return cancellationReason; }
}
