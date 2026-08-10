package com.portfolio.eventsourcing.infrastructure.persistence;

import com.portfolio.eventsourcing.application.OrderView;
import com.portfolio.eventsourcing.application.port.out.OrderProjectionStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcOrderProjectionStore implements OrderProjectionStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public JdbcOrderProjectionStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Override
    public void replaceAll(Map<UUID, OrderView> orders, long eventCount) {
        transaction.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM order_projection");
            jdbc.batchUpdate("""
                INSERT INTO order_projection (
                    order_id, customer_name, product, quantity, amount_minor, currency,
                    status, tracking_number, cancellation_reason, payment_id, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """, orders.values(), 250, (statement, order) -> {
                    statement.setObject(1, order.orderId());
                    statement.setString(2, order.customerName());
                    statement.setString(3, order.product());
                    statement.setInt(4, order.quantity());
                    statement.setLong(5, order.amountMinor());
                    statement.setString(6, order.currency());
                    statement.setString(7, order.status());
                    statement.setString(8, order.trackingNumber());
                    statement.setString(9, order.cancellationReason());
                    statement.setObject(10, order.paymentId());
                });
            jdbc.update("""
                INSERT INTO projection_checkpoint (projection_name, event_count, rebuilt_at)
                VALUES ('orders', ?, now())
                ON CONFLICT (projection_name)
                DO UPDATE SET event_count = excluded.event_count, rebuilt_at = excluded.rebuilt_at
                """, eventCount);
        });
    }

    @Override
    public Optional<OrderView> findById(UUID orderId) {
        return jdbc.query("SELECT * FROM order_projection WHERE order_id = ?", this::mapOrder, orderId)
            .stream().findFirst();
    }

    @Override
    public List<OrderView> findAll() {
        return jdbc.query("SELECT * FROM order_projection ORDER BY order_id", this::mapOrder);
    }

    @Override
    public long countByStatus(String status) {
        Long count = jdbc.queryForObject(
            "SELECT count(*) FROM order_projection WHERE status = ?", Long.class, status);
        return count == null ? 0 : count;
    }

    @Override
    public long checkpoint() {
        return jdbc.query("""
            SELECT event_count FROM projection_checkpoint WHERE projection_name = 'orders'
            """, resultSet -> resultSet.next() ? resultSet.getLong(1) : 0L);
    }

    private OrderView mapOrder(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new OrderView(
            resultSet.getObject("order_id", UUID.class),
            resultSet.getString("customer_name"),
            resultSet.getString("product"),
            resultSet.getInt("quantity"),
            resultSet.getLong("amount_minor"),
            resultSet.getString("currency"),
            resultSet.getString("status"),
            resultSet.getString("tracking_number"),
            resultSet.getString("cancellation_reason"),
            resultSet.getObject("payment_id", UUID.class));
    }
}
