CREATE TABLE transfer_ledger (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL UNIQUE,
    source_wallet_id UUID NOT NULL,
    target_wallet_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL,
    transferred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ledger_source ON transfer_ledger (source_wallet_id, transferred_at);
CREATE INDEX idx_ledger_target ON transfer_ledger (target_wallet_id, transferred_at);

ALTER TABLE wallets ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR';