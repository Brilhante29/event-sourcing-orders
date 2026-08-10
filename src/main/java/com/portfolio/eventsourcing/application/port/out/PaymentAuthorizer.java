package com.portfolio.eventsourcing.application.port.out;

import java.util.UUID;

public interface PaymentAuthorizer {
    PaymentAuthorization authorize(PaymentAuthorizationRequest request);

    record PaymentAuthorizationRequest(UUID orderId, long amountMinor, String currency) {
        public String merchantReference() {
            return orderId.toString();
        }

        public String idempotencyKey() {
            return "order:" + orderId + ":authorize:v1";
        }
    }

    record PaymentAuthorization(UUID paymentId, String status, boolean replayed) {}
}
