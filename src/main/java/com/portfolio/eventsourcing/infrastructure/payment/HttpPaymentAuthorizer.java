package com.portfolio.eventsourcing.infrastructure.payment;

import com.portfolio.eventsourcing.application.port.out.PaymentAuthorizer;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "payments.adapter", havingValue = "http")
public final class HttpPaymentAuthorizer implements PaymentAuthorizer {
    private final RestClient client;

    public HttpPaymentAuthorizer(RestClient.Builder builder, @Value("${payments.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public PaymentAuthorization authorize(PaymentAuthorizationRequest request) {
        ResponseEntity<PaymentResponse> response = client.post()
            .uri("/v1/payments")
            .header("Idempotency-Key", request.idempotencyKey())
            .body(Map.of(
                "amount_minor", request.amountMinor(),
                "currency", request.currency(),
                "merchant_reference", request.merchantReference()))
            .retrieve()
            .toEntity(PaymentResponse.class);

        int status = response.getStatusCode().value();
        if (status != 200 && status != 201) {
            throw new IllegalStateException("Payment authorization returned HTTP " + status);
        }
        PaymentResponse body = response.getBody();
        if (body == null || body.id() == null) {
            throw new IllegalStateException("Payment authorization returned no payment id");
        }
        return new PaymentAuthorization(body.id(), body.status(), Boolean.TRUE.equals(body.replayed()));
    }

    private record PaymentResponse(UUID id, String status, Boolean replayed) {}
}
