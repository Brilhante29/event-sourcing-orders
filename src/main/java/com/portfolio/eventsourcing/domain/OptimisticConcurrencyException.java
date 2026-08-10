package com.portfolio.eventsourcing.domain;

import java.util.UUID;

public final class OptimisticConcurrencyException extends RuntimeException {
    public OptimisticConcurrencyException(UUID aggregateId, long expectedVersion) {
        super("Stream " + aggregateId + " was not at expected version " + expectedVersion);
    }

    public OptimisticConcurrencyException(UUID aggregateId, long expectedVersion, Throwable cause) {
        super("Stream " + aggregateId + " was not at expected version " + expectedVersion, cause);
    }
}
