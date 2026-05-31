CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    owner_name VARCHAR(255) NOT NULL UNIQUE,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY ,
    aggregate_type VARCHAR(100) NOT NULL ,
    aggregate_id UUID NOT NULL ,
    event_type VARCHAR(100) NOT NULL ,
    payload TEXT NOT NULL ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status ON outbox_events (status, created_at);
CREATE INDEX idx_wallets_owner ON wallets(owner_name);