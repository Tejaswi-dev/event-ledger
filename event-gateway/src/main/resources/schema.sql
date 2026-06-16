CREATE TABLE IF NOT EXISTS events (
    event_id        VARCHAR(255)   NOT NULL,
    account_id      VARCHAR(255)   NOT NULL,
    type            VARCHAR(10)    NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    currency        VARCHAR(10)    NOT NULL,
    event_timestamp TIMESTAMP      NOT NULL,
    metadata        TEXT,
    received_at     TIMESTAMP      NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (event_id)
);

CREATE INDEX IF NOT EXISTS idx_events_account_timestamp ON events (account_id, event_timestamp);
