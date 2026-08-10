package com.portfolio.eventsourcing.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.eventsourcing.application.port.out.PaymentAuthorizer.PaymentAuthorizationRequest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpPaymentAuthorizerTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldHonorPaymentsContractAndAcceptCreated() throws Exception {
        var paymentId = UUID.randomUUID();
        var capturedBody = new AtomicReference<String>();
        var capturedKey = new AtomicReference<String>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/payments", exchange -> {
            capturedKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"id\":\"" + paymentId
                + "\",\"status\":\"AUTHORIZED\",\"replayed\":false}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        var orderId = UUID.randomUUID();
        var authorizer = new HttpPaymentAuthorizer(
            RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort());
        var authorization = authorizer.authorize(
            new PaymentAuthorizationRequest(orderId, 2590, "BRL"));

        assertThat(authorization.paymentId()).isEqualTo(paymentId);
        assertThat(capturedKey.get()).isEqualTo("order:" + orderId + ":authorize:v1");
        assertThat(capturedBody.get()).contains("\"merchant_reference\":\"" + orderId + "\"");
        assertThat(capturedBody.get()).contains("\"amount_minor\":2590");
        assertThat(capturedBody.get()).contains("\"currency\":\"BRL\"");
    }

    @Test
    void localAdapterShouldBeDeterministicForTheIdempotencyKey() {
        var orderId = UUID.randomUUID();
        var request = new PaymentAuthorizationRequest(orderId, 1000, "BRL");
        var adapter = new LocalPaymentAuthorizer();

        assertThat(adapter.authorize(request).paymentId())
            .isEqualTo(adapter.authorize(request).paymentId());
    }
}
