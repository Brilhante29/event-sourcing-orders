package com.portfolio.eventsourcing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.eventsourcing.application.CqrsProjection;
import com.portfolio.eventsourcing.domain.OptimisticConcurrencyException;
import com.portfolio.eventsourcing.domain.OrderCommand;
import com.portfolio.eventsourcing.domain.OrderEvent;
import com.portfolio.eventsourcing.domain.OrderService;
import com.portfolio.eventsourcing.infrastructure.EventSerializer;
import com.portfolio.eventsourcing.infrastructure.payment.LocalPaymentAuthorizer;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class PostgreSqlEventStoreTest {
    private JdbcTemplate jdbc;
    private JdbcOrderEventRepository repository;
    private JdbcOrderProjectionStore projectionStore;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
            System.getenv("TEST_DATABASE_URL"),
            environment("TEST_DATABASE_USER", "orders"),
            environment("TEST_DATABASE_PASSWORD", "orders"));
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("TRUNCATE order_events, order_projection, projection_checkpoint RESTART IDENTITY");
        repository = new JdbcOrderEventRepository(jdbc, new EventSerializer());
        projectionStore = new JdbcOrderProjectionStore(
            jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @Test
    void shouldSurviveRepositoryAndProjectionRestart() {
        var service = new OrderService(repository, new LocalPaymentAuthorizer());
        var orderId = UUID.randomUUID();
        service.handle(new OrderCommand.CreateOrder(orderId, "Ada", "Keyboard", 1, 2590, "BRL"));
        service.handle(new OrderCommand.AuthorizePayment(orderId));
        service.handle(new OrderCommand.ShipOrder(orderId, "TRACK-1"));

        var projection = new CqrsProjection(repository, projectionStore);
        assertThat(projection.rebuild()).isEqualTo(3);

        var restartedRepository = new JdbcOrderEventRepository(jdbc, new EventSerializer());
        var restartedProjection = new CqrsProjection(restartedRepository, projectionStore);

        assertThat(restartedRepository.findStream(orderId)).hasSize(3);
        assertThat(restartedProjection.getOrder(orderId).status()).isEqualTo("SHIPPED");
        assertThat(restartedProjection.getOrder(orderId).paymentId()).isNotNull();
        assertThat(restartedProjection.checkpoint()).isEqualTo(3);
    }

    @Test
    void shouldRebuildDeletedReadModelFromPersistedHistory() {
        var service = new OrderService(repository, new LocalPaymentAuthorizer());
        var orderId = UUID.randomUUID();
        service.handle(new OrderCommand.CreateOrder(orderId, "Grace", "Monitor", 2, 5000, "USD"));
        service.handle(new OrderCommand.AuthorizePayment(orderId));
        service.handle(new OrderCommand.ShipOrder(orderId, "TRACK-2"));
        service.handle(new OrderCommand.DeliverOrder(orderId));
        jdbc.update("DELETE FROM order_projection");

        var projection = new CqrsProjection(repository, projectionStore);
        assertThat(projection.rebuild()).isEqualTo(4);
        assertThat(projection.getOrder(orderId).status()).isEqualTo("DELIVERED");
        assertThat(projection.checkpoint()).isEqualTo(4);
    }

    @Test
    void shouldRejectStaleExpectedVersionAndKeepUniqueSequence() {
        var orderId = UUID.randomUUID();
        var first = new OrderEvent.OrderCreated(
            orderId, "Linus", "Laptop", 1, 9000, "USD", Instant.now());
        repository.append(first, 0, orderId, UUID.randomUUID());

        assertThatThrownBy(() -> repository.append(
            new OrderEvent.OrderCancelled(orderId, "stale command", Instant.now()),
            0, orderId, UUID.randomUUID()))
            .isInstanceOf(OptimisticConcurrencyException.class);

        assertThat(repository.findStream(orderId)).extracting(event -> event.sequence())
            .containsExactly(1L);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM order_events WHERE aggregate_id = ?", Long.class, orderId))
            .isEqualTo(1L);
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
