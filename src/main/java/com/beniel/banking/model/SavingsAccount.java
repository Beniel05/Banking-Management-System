package com.beniel.banking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SavingsAccount extends BankAccount {
    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.04");

    public SavingsAccount(String accountNumber, long customerId, String customerName, BigDecimal balance) {
        super(accountNumber, customerId, customerName, balance);
    }

    @Override
    public BigDecimal calculateInterest() {
        return getBalance().multiply(ANNUAL_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
