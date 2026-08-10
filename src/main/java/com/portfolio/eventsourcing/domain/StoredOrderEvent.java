package com.portfolio.eventsourcing.domain;

import java.time.Instant;
import java.util.UUID;

public record StoredOrderEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    UUID aggregateId,
    long sequence,
    UUID correlationId,
    UUID causationId,
    Instant occurredAt,
    OrderEvent payload
) {}
