package com.beniel.banking.model;

import java.math.BigDecimal;

public class CurrentAccount extends BankAccount {
    public CurrentAccount(String accountNumber, long customerId, String customerName, BigDecimal balance) {
        super(accountNumber, customerId, customerName, balance);
    }

    @Override
    public BigDecimal calculateInterest() {
        return BigDecimal.ZERO.setScale(2);
    }
}
