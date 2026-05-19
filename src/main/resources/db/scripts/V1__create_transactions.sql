CREATE TABLE transactions (
                              id          VARCHAR(36)    NOT NULL PRIMARY KEY,
                              account_id  VARCHAR(36)    NOT NULL,
                              amount      NUMERIC(19, 4) NOT NULL,
                              timestamp   TIMESTAMP      NOT NULL,
                              description VARCHAR(255),
                              category    VARCHAR(50)    NOT NULL
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
