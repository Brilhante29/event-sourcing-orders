package com.portfolio.eventsourcing.application;

import com.portfolio.eventsourcing.domain.OrderEvent;
import com.portfolio.eventsourcing.domain.OrderEventRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class EventStore implements OrderEventRepository {
    private final List<OrderEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void append(OrderEvent event) {
        events.add(event);
    }

    @Override
    public List<OrderEvent> findByOrderId(UUID orderId) {
        return events.stream()
            .filter(e -> e.orderId().equals(orderId))
            .toList();
    }

    @Override
    public List<OrderEvent> findAll() {
        return List.copyOf(events);
    }
}
