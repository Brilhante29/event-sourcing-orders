package com.portfolio.eventsourcing.application.port.out;

import com.portfolio.eventsourcing.application.OrderView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OrderProjectionStore {
    void replaceAll(Map<UUID, OrderView> orders, long eventCount);
    Optional<OrderView> findById(UUID orderId);
    List<OrderView> findAll();
    long countByStatus(String status);
    long checkpoint();
}
