CREATE TABLE wee_user (
    id UUID PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL UNIQUE,
    balance_eur NUMERIC(19, 2) NOT NULL DEFAULT 100.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bet (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES wee_user (id),
    event_id VARCHAR(512) NOT NULL,
    market_key VARCHAR(128) NOT NULL,
    outcome_id VARCHAR(64) NOT NULL,
    stake_eur NUMERIC(19, 2) NOT NULL,
    odds INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    payout_eur NUMERIC(19, 2),
    placed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    settled_at TIMESTAMPTZ
);

CREATE INDEX idx_bet_user ON bet (user_id);
CREATE INDEX idx_bet_event ON bet (event_id);
