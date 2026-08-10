package com.portfolio.eventsourcing.domain;

import java.util.List;
import java.util.UUID;

public interface OrderEventRepository {
    StoredOrderEvent append(
        OrderEvent event,
        long expectedVersion,
        UUID correlationId,
        UUID causationId
    );

    List<StoredOrderEvent> findStream(UUID orderId);
    List<StoredOrderEvent> findAll();

    default List<OrderEvent> findByOrderId(UUID orderId) {
        return findStream(orderId).stream().map(StoredOrderEvent::payload).toList();
    }
}
