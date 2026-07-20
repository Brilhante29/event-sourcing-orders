package com.portfolio.eventsourcing.domain;

import java.util.List;
import java.util.UUID;

public interface OrderEventRepository {
    void append(OrderEvent event);
    List<OrderEvent> findByOrderId(UUID orderId);
    List<OrderEvent> findAll();
}
