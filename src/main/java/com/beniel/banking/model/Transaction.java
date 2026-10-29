package com.beniel.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    private final long transactionId;
    private final String accountNumber;
    private final String customerName;
    private final String type;
    private final BigDecimal amount;
    private final String description;
    private final LocalDateTime transactionTime;

    public Transaction(
            long transactionId,
            String accountNumber,
            String type,
            BigDecimal amount,
            String description,
            LocalDateTime transactionTime
    ) {
        this(
                transactionId,
                accountNumber,
                null,
                type,
                amount,
                description,
                transactionTime
        );
    }

    public Transaction(
            long transactionId,
            String accountNumber,
            String customerName,
            String type,
            BigDecimal amount,
            String description,
            LocalDateTime transactionTime
    ) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.transactionTime = transactionTime;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    @Override
    public String toString() {
        String formattedTime = transactionTime.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        return String.format(
                "#%d | Account: %s | Customer: %s | Type: %s | Amount: %s | Description: %s | Time: %s",
                transactionId,
                accountNumber,
                customerName == null ? "N/A" : customerName,
                type,
                amount,
                description == null ? "N/A" : description,
                formattedTime
        );
    }
}