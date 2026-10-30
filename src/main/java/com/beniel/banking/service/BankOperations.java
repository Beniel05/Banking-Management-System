package com.beniel.banking.service;

import com.beniel.banking.model.BankAccount;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BankOperations {
    String createAccount(String name, String email, char[] password, String accountType, BigDecimal openingBalance)
            throws SQLException;
    Optional<BankAccount> findAccount(String accountNumber) throws SQLException;
    void deposit(String accountNumber, BigDecimal amount) throws SQLException;
    void withdraw(String accountNumber, BigDecimal amount) throws SQLException;
    void transfer(String fromAccount, String toAccount, BigDecimal amount) throws SQLException;
    List<BankAccount> getAllAccounts() throws SQLException;
}
