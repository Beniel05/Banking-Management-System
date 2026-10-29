package com.beniel.banking.model;

import java.math.BigDecimal;

public abstract class BankAccount {
    private final String accountNumber;
    private final long customerId;
    private final String customerName;
    private BigDecimal balance;

    protected BankAccount(String accountNumber, long customerId, String customerName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        setBalance(balance);
    }

    public abstract BigDecimal calculateInterest();

    public String getAccountNumber() { return accountNumber; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getBalance() { return balance; }

    // Package-independent setter is intentionally omitted: balance changes go through service/DAO operations.
    protected final void setBalance(BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        this.balance = balance.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{accountNumber='" + accountNumber
                + "', customer='" + customerName + "', balance=" + balance + "}";
    }
}
