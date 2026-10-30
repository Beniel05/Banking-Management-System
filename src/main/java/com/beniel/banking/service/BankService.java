package com.beniel.banking.service;

import com.beniel.banking.dao.AccountDAO;
import com.beniel.banking.dao.CustomerDAO;
import com.beniel.banking.dao.TransactionDAO;
import com.beniel.banking.exception.AccountNotFoundException;
import com.beniel.banking.exception.InsufficientBalanceException;
import com.beniel.banking.model.BankAccount;
import com.beniel.banking.model.Role;
import com.beniel.banking.model.Transaction;
import com.beniel.banking.util.JDBCUtil;
import com.beniel.banking.util.PasswordHasher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BankService implements BankOperations {

    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public BankService(
            CustomerDAO customerDAO,
            AccountDAO accountDAO,
            TransactionDAO transactionDAO
    ) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    @Override
    public String createAccount(
            String name,
            String email,
            char[] password,
            String accountType,
            BigDecimal openingBalance
    ) throws SQLException {

        validateAmount(openingBalance, true);

        if (password == null || password.length == 0) {
            throw new IllegalArgumentException(
                    "Password is required."
            );
        }

        String type = accountType == null
                ? ""
                : accountType.trim().toUpperCase();

        if (!type.equals("SAVINGS") && !type.equals("CURRENT")) {
            throw new IllegalArgumentException(
                    "Account type must be SAVINGS or CURRENT."
            );
        }

        // Public registration must never create an administrator.
        Role role = JDBCUtil.isAdminEmail(email)
                ? Role.ADMIN
                : Role.CUSTOMER;

        String passwordHash = PasswordHasher.hash(password);

        try (Connection connection = JDBCUtil.getConnection()) {

            connection.setAutoCommit(false);

            try {
                long customerId = customerDAO.nextCustomerId(connection);

                customerDAO.insert(
                        connection,
                        customerId,
                        name,
                        email,
                        passwordHash,
                        role
                );

                String accountNumber = "ACC"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase();

                accountDAO.insert(
                        connection,
                        accountNumber,
                        customerId,
                        type,
                        openingBalance
                );

                if (openingBalance.compareTo(BigDecimal.ZERO) > 0) {
                    transactionDAO.insert(
                            connection,
                            accountNumber,
                            "DEPOSIT",
                            openingBalance,
                            "Opening balance"
                    );
                }

                connection.commit();

                return accountNumber;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public Optional<BankAccount> authenticate(
            String email,
            char[] password
    ) throws SQLException {

        if (password == null) {
            return Optional.empty();
        }

        try {
            if (email == null || email.trim().isEmpty()) {
                return Optional.empty();
            }

            Optional<CustomerDAO.CustomerCredentials> credentials =
                    customerDAO.findCredentialsByEmail(email);

            if (credentials.isEmpty()) {
                return Optional.empty();
            }

            CustomerDAO.CustomerCredentials customer =
                    credentials.get();

            if (!PasswordHasher.verify(
                    password,
                    customer.getPasswordHash()
            )) {
                return Optional.empty();
            }

            return accountDAO.findByCustomerId(
                    customer.getCustomerId()
            );

        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Retrieves the persisted role for a customer.
     *
     * Call this only after successful authentication.
     */
    public Optional<Role> getRoleByEmail(String email)
            throws SQLException {

        return customerDAO.findCredentialsByEmail(email)
                .map(CustomerDAO.CustomerCredentials::getRole);
    }

    @Override
    public Optional<BankAccount> findAccount(
            String accountNumber
    ) throws SQLException {

        return accountDAO.findByNumber(accountNumber);
    }

    public BankAccount getAccount(String accountNumber)
            throws SQLException {

        return findAccount(accountNumber).orElseThrow(
                () -> new AccountNotFoundException(
                        "No account found: " + accountNumber
                )
        );
    }

    @Override
    public void deposit(
            String accountNumber,
            BigDecimal amount
    ) throws SQLException {

        validateAmount(amount, false);

        try (Connection connection = JDBCUtil.getConnection()) {

            connection.setAutoCommit(false);

            try {
                BankAccount account =
                        accountDAO.findByNumber(connection, accountNumber)
                                .orElseThrow(
                                        () -> new AccountNotFoundException(
                                                "No account found: "
                                                        + accountNumber
                                        )
                                );

                BigDecimal newBalance =
                        account.getBalance().add(amount);

                accountDAO.updateBalance(
                        connection,
                        accountNumber,
                        newBalance
                );

                transactionDAO.insert(
                        connection,
                        accountNumber,
                        "DEPOSIT",
                        amount,
                        "Cash deposit"
                );

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Override
    public void withdraw(
            String accountNumber,
            BigDecimal amount
    ) throws SQLException {

        validateAmount(amount, false);

        try (Connection connection = JDBCUtil.getConnection()) {

            connection.setAutoCommit(false);

            try {
                BankAccount account =
                        accountDAO.findByNumber(connection, accountNumber)
                                .orElseThrow(
                                        () -> new AccountNotFoundException(
                                                "No account found: "
                                                        + accountNumber
                                        )
                                );

                if (account.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientBalanceException(
                            "Available balance is "
                                    + account.getBalance()
                    );
                }

                accountDAO.updateBalance(
                        connection,
                        accountNumber,
                        account.getBalance().subtract(amount)
                );

                transactionDAO.insert(
                        connection,
                        accountNumber,
                        "WITHDRAWAL",
                        amount,
                        "Cash withdrawal"
                );

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Override
    public void transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount
    ) throws SQLException {

        validateAmount(amount, false);

        if (fromAccount == null || toAccount == null) {
            throw new IllegalArgumentException(
                    "Source and destination accounts are required."
            );
        }

        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException(
                    "Source and destination must differ."
            );
        }

        try (Connection connection = JDBCUtil.getConnection()) {

            connection.setAutoCommit(false);

            try {
                BankAccount source =
                        accountDAO.findByNumber(connection, fromAccount)
                                .orElseThrow(
                                        () -> new AccountNotFoundException(
                                                "Source account not found."
                                        )
                                );

                BankAccount destination =
                        accountDAO.findByNumber(connection, toAccount)
                                .orElseThrow(
                                        () -> new AccountNotFoundException(
                                                "Destination account not found."
                                        )
                                );

                if (source.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientBalanceException(
                            "Available balance is "
                                    + source.getBalance()
                    );
                }

                accountDAO.updateBalance(
                        connection,
                        fromAccount,
                        source.getBalance().subtract(amount)
                );

                accountDAO.updateBalance(
                        connection,
                        toAccount,
                        destination.getBalance().add(amount)
                );

                transactionDAO.insert(
                        connection,
                        fromAccount,
                        "TRANSFER_OUT",
                        amount,
                        "Transfer to " + toAccount
                );

                transactionDAO.insert(
                        connection,
                        toAccount,
                        "TRANSFER_IN",
                        amount,
                        "Transfer from " + fromAccount
                );

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Override
    public List<BankAccount> getAllAccounts() throws SQLException {
        return accountDAO.findAll();
    }

    public List<Transaction> getTransactions(
            String accountNumber
    ) throws SQLException {

        if (accountDAO.findByNumber(accountNumber).isEmpty()) {
            throw new AccountNotFoundException(
                    "No account found: " + accountNumber
            );
        }

        return transactionDAO.findByAccount(accountNumber);
    }

    public List<BankAccount> findHighBalanceAccounts(
            List<BankAccount> accounts,
            BigDecimal threshold
    ) {

        Predicate<BankAccount> predicate =
                account -> account.getBalance().compareTo(threshold) > 0;

        return accounts.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactions() throws SQLException {
        return transactionDAO.findAll();
    }

    private void validateAmount(
            BigDecimal amount,
            boolean allowZero
    ) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Amount is required."
            );
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amounts may have at most two decimal places."
            );
        }

        int comparison = amount.compareTo(BigDecimal.ZERO);

        if (comparison < 0 || (!allowZero && comparison == 0)) {
            throw new IllegalArgumentException(
                    allowZero
                            ? "Amount cannot be negative."
                            : "Amount must be greater than zero."
            );
        }
    }
}