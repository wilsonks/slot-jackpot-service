CREATE TABLE jackpots (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    base_amount NUMERIC(19,4) NOT NULL,
    increment_rate NUMERIC(10,6) NOT NULL,
    current_amount NUMERIC(19,4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    won_by VARCHAR(100),
    won_amount NUMERIC(19,4),
    won_at TIMESTAMPTZ,
    last_incremented_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);
