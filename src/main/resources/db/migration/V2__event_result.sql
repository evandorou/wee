CREATE TABLE event_result (
    event_id VARCHAR(512) PRIMARY KEY,
    winning_driver_number INTEGER NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);
