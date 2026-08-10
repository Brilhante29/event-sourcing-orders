package com.portfolio.eventsourcing.application;

import com.portfolio.eventsourcing.application.port.out.OrderProjectionStore;
import com.portfolio.eventsourcing.domain.Order;
import com.portfolio.eventsourcing.domain.OrderEventRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CqrsProjection {
    private final OrderEventRepository repository;
    private final OrderProjectionStore store;

    public CqrsProjection(OrderEventRepository repository, OrderProjectionStore store) {
        this.repository = repository;
        this.store = store;
    }

    public long rebuild() {
        var events = repository.findAll();
        var orders = new LinkedHashMap<UUID, Order>();
        for (var storedEvent : events) {
            orders.compute(storedEvent.aggregateId(), (id, order) -> {
                var current = order == null ? new Order() : order;
                current.apply(storedEvent.payload());
                return current;
            });
        }
        var views = new LinkedHashMap<UUID, OrderView>();
        orders.forEach((id, order) -> views.put(id, OrderView.from(order)));
        store.replaceAll(views, events.size());
        return events.size();
    }

    public OrderView getOrder(UUID orderId) {
        return store.findById(orderId).orElse(null);
    }

    public List<OrderView> getAllOrders() {
        return store.findAll();
    }

    public long countByStatus(String status) {
        return store.countByStatus(status);
    }

    public long checkpoint() {
        return store.checkpoint();
    }
}
