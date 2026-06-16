CREATE TABLE IF NOT EXISTS accounts (
    account_id  VARCHAR(255)   NOT NULL,
    balance     DECIMAL(19, 4) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP      NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (account_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    event_id        VARCHAR(255)   NOT NULL,
    account_id      VARCHAR(255)   NOT NULL,
    type            VARCHAR(10)    NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    event_timestamp TIMESTAMP      NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (event_id)
);

CREATE INDEX IF NOT EXISTS idx_tx_account_timestamp ON transactions (account_id, event_timestamp);
