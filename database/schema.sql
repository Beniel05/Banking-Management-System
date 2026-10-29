CREATE DATABASE IF NOT EXISTS banking_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE banking_management;

CREATE TABLE IF NOT EXISTS customers (
                                         customer_id BIGINT PRIMARY KEY,
                                         name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',

    -- Only one customer can have ADMIN role.
    -- CUSTOMER rows receive NULL, which can repeat in a UNIQUE index.
    admin_guard TINYINT
    GENERATED ALWAYS AS (
                            CASE WHEN role = 'ADMIN' THEN 1 ELSE NULL END
                        ) STORED,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_single_admin UNIQUE (admin_guard)
    );

CREATE TABLE IF NOT EXISTS customer_id_sequence (
                                                    sequence_name VARCHAR(30) PRIMARY KEY,
    last_id BIGINT NOT NULL
    ) ENGINE = InnoDB;

INSERT IGNORE INTO customer_id_sequence (sequence_name, last_id)
SELECT 'customer', COALESCE(MAX(customer_id), 0)
FROM customers;

UPDATE customer_id_sequence
SET last_id = GREATEST(
        last_id,
        (SELECT COALESCE(MAX(customer_id), 0) FROM customers)
              )
WHERE sequence_name = 'customer';

CREATE TABLE IF NOT EXISTS accounts (
                                        account_number VARCHAR(20) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_accounts_customer
    FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id),

    CONSTRAINT chk_account_type
    CHECK (account_type IN ('SAVINGS', 'CURRENT')),

    CONSTRAINT chk_account_balance
    CHECK (balance >= 0)
    );

CREATE TABLE IF NOT EXISTS transactions (
                                            transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            account_number VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(255),
    transaction_time DATETIME NOT NULL,

    CONSTRAINT fk_transactions_account
    FOREIGN KEY (account_number)
    REFERENCES accounts(account_number),

    CONSTRAINT chk_transaction_amount
    CHECK (amount > 0)
    );

CREATE INDEX idx_transactions_account_time
    ON transactions (account_number, transaction_time);