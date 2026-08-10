package com.portfolio.eventsourcing.infrastructure.persistence;

import com.portfolio.eventsourcing.domain.OptimisticConcurrencyException;
import com.portfolio.eventsourcing.domain.OrderEvent;
import com.portfolio.eventsourcing.domain.OrderEventRepository;
import com.portfolio.eventsourcing.domain.StoredOrderEvent;
import com.portfolio.eventsourcing.infrastructure.EventSerializer;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrderEventRepository implements OrderEventRepository {
    private final JdbcTemplate jdbc;
    private final EventSerializer serializer;

    public JdbcOrderEventRepository(JdbcTemplate jdbc, EventSerializer serializer) {
        this.jdbc = jdbc;
        this.serializer = serializer;
    }

    @Override
    public StoredOrderEvent append(
        OrderEvent event,
        long expectedVersion,
        UUID correlationId,
        UUID causationId
    ) {
        UUID eventId = UUID.randomUUID();
        long sequence = expectedVersion + 1;
        String eventType = event.getClass().getSimpleName();
        try {
            int inserted = jdbc.update("""
                INSERT INTO order_events (
                    event_id, aggregate_id, sequence, event_type, event_version,
                    correlation_id, causation_id, occurred_at, payload
                )
                SELECT ?, ?, ?, ?, 1, ?, ?, ?, ?::jsonb
                WHERE (SELECT COALESCE(MAX(sequence), 0) FROM order_events WHERE aggregate_id = ?) = ?
                """,
                eventId, event.orderId(), sequence, eventType, correlationId, causationId,
                Timestamp.from(event.timestamp()), serializer.serialize(event), event.orderId(), expectedVersion);
            if (inserted != 1) {
                throw new OptimisticConcurrencyException(event.orderId(), expectedVersion);
            }
        } catch (DuplicateKeyException exception) {
            throw new OptimisticConcurrencyException(event.orderId(), expectedVersion, exception);
        }
        return new StoredOrderEvent(
            eventId, eventType, 1, event.orderId(), sequence, correlationId,
            causationId, event.timestamp(), event);
    }

    @Override
    public List<StoredOrderEvent> findStream(UUID orderId) {
        return jdbc.query("""
            SELECT event_id, event_type, event_version, aggregate_id, sequence,
                   correlation_id, causation_id, occurred_at, payload::text
            FROM order_events
            WHERE aggregate_id = ?
            ORDER BY sequence
            """, this::mapEvent, orderId);
    }

    @Override
    public List<StoredOrderEvent> findAll() {
        return jdbc.query("""
            SELECT event_id, event_type, event_version, aggregate_id, sequence,
                   correlation_id, causation_id, occurred_at, payload::text
            FROM order_events
            ORDER BY global_position
            """, this::mapEvent);
    }

    private StoredOrderEvent mapEvent(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new StoredOrderEvent(
            resultSet.getObject("event_id", UUID.class),
            resultSet.getString("event_type"),
            resultSet.getInt("event_version"),
            resultSet.getObject("aggregate_id", UUID.class),
            resultSet.getLong("sequence"),
            resultSet.getObject("correlation_id", UUID.class),
            resultSet.getObject("causation_id", UUID.class),
            resultSet.getTimestamp("occurred_at").toInstant(),
            serializer.deserialize(resultSet.getString("payload")));
    }
}
