CREATE TABLE event_log(
    id UUID PRIMARY KEY ,
    transfer_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL ,
    source_wallet_id UUID NOT NULL ,
    target_wallet_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL ,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    source_owner VARCHAR(255),
    target_owner VARCHAR(255),
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL ,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_event_log_source on event_log (source_wallet_id);
CREATE INDEX idx_event_log_target on event_log (target_wallet_id);
CREATE INDEX idx_event_log_recorded on event_log (recorded_at DESC);