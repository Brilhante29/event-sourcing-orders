package com.portfolio.eventsourcing.application;

import com.portfolio.eventsourcing.domain.Order;
import com.portfolio.eventsourcing.domain.OrderEvent;
import com.portfolio.eventsourcing.domain.OrderEventRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CqrsProjection {
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();
    private final OrderEventRepository repository;

    public CqrsProjection(OrderEventRepository repository) {
        this.repository = repository;
    }

    public void rebuild() {
        orders.clear();
        for (var event : repository.findAll()) {
            orders.compute(event.orderId(), (id, order) -> {
                if (order == null) order = new Order();
                order.apply(event);
                return order;
            });
        }
    }

    public Order getOrder(UUID orderId) {
        return orders.get(orderId);
    }

    public List<Order> getAllOrders() {
        return List.copyOf(orders.values());
    }

    public int countByStatus(String status) {
        return (int) orders.values().stream()
            .filter(o -> status.equals(o.getStatus()))
            .count();
    }
}
