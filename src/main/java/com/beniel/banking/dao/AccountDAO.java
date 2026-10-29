package com.beniel.banking.dao;

import com.beniel.banking.model.BankAccount;
import com.beniel.banking.model.CurrentAccount;
import com.beniel.banking.model.SavingsAccount;
import com.beniel.banking.util.JDBCUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDAO {

    public void insert(
            Connection connection,
            String number,
            long customerId,
            String type,
            BigDecimal balance
    ) throws SQLException {

        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, balance) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);
            statement.setLong(2, customerId);
            statement.setString(3, type);
            statement.setBigDecimal(4, balance);

            statement.executeUpdate();
        }
    }

    public Optional<BankAccount> findByNumber(String number)
            throws SQLException {

        try (Connection connection = JDBCUtil.getConnection()) {
            return findByNumber(connection, number);
        }
    }

    public Optional<BankAccount> findByNumber(
            Connection connection,
            String number
    ) throws SQLException {

        String sql = "SELECT a.account_number, a.customer_id, c.name, a.account_type, a.balance FROM accounts a JOIN customers c ON c.customer_id = a.customer_id WHERE a.account_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapAccount(rs));
            }
        }
    }

    public Optional<BankAccount> findByCustomerId(long customerId)
            throws SQLException {

        String sql = "SELECT a.account_number, a.customer_id, c.name, a.account_type, a.balance FROM accounts a JOIN customers c ON c.customer_id = a.customer_id WHERE a.customer_id = ?";

        try (Connection connection = JDBCUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapAccount(rs));
            }
        }
    }

    public List<BankAccount> findAll() throws SQLException {

        List<BankAccount> accounts = new ArrayList<>();

        String sql = "SELECT a.account_number, a.customer_id, c.name, a.account_type, a.balance FROM accounts a JOIN customers c ON c.customer_id = a.customer_id ORDER BY a.created_at DESC";

        try (Connection connection = JDBCUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapAccount(rs));
            }
        }

        return accounts;
    }

    private BankAccount mapAccount(ResultSet rs) throws SQLException {

        String type = rs.getString("account_type");
        String accountNumber = rs.getString("account_number");
        long customerId = rs.getLong("customer_id");
        String name = rs.getString("name");
        BigDecimal balance = rs.getBigDecimal("balance");

        if ("SAVINGS".equals(type)) {
            return new SavingsAccount(
                    accountNumber,
                    customerId,
                    name,
                    balance
            );
        }

        if ("CURRENT".equals(type)) {
            return new CurrentAccount(
                    accountNumber,
                    customerId,
                    name,
                    balance
            );
        }

        throw new SQLException("Unsupported account type: " + type);
    }

    public void updateBalance(
            Connection connection,
            String number,
            BigDecimal balance
    ) throws SQLException {

        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, balance);
            statement.setString(2, number);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Balance update did not affect exactly one account."
                );
            }
        }
    }
}