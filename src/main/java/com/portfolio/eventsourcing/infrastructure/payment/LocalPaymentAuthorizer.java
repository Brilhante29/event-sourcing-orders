package com.portfolio.eventsourcing.infrastructure.payment;

import com.portfolio.eventsourcing.application.port.out.PaymentAuthorizer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payments.adapter", havingValue = "local", matchIfMissing = true)
public final class LocalPaymentAuthorizer implements PaymentAuthorizer {
    @Override
    public PaymentAuthorization authorize(PaymentAuthorizationRequest request) {
        UUID paymentId = UUID.nameUUIDFromBytes(
            request.idempotencyKey().getBytes(StandardCharsets.UTF_8));
        return new PaymentAuthorization(paymentId, "AUTHORIZED", false);
    }
}
