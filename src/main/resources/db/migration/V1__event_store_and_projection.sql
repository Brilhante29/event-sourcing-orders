CREATE TABLE order_events (
    global_position BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    aggregate_id UUID NOT NULL,
    sequence BIGINT NOT NULL CHECK (sequence > 0),
    event_type VARCHAR(96) NOT NULL,
    event_version INTEGER NOT NULL CHECK (event_version > 0),
    correlation_id UUID NOT NULL,
    causation_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    UNIQUE (aggregate_id, sequence)
);

CREATE INDEX order_events_aggregate_position_idx
    ON order_events (aggregate_id, sequence);

CREATE TABLE order_projection (
    order_id UUID PRIMARY KEY,
    customer_name VARCHAR(160) NOT NULL,
    product VARCHAR(160) NOT NULL,
    quantity INTEGER NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tracking_number VARCHAR(160),
    cancellation_reason VARCHAR(500),
    payment_id UUID,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE projection_checkpoint (
    projection_name VARCHAR(96) PRIMARY KEY,
    event_count BIGINT NOT NULL,
    rebuilt_at TIMESTAMPTZ NOT NULL
);
