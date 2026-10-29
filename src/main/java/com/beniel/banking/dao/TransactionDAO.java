package com.beniel.banking.dao;

import com.beniel.banking.model.Transaction;
import com.beniel.banking.util.JDBCUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    public void insert(Connection connection, String accountNumber, String type, BigDecimal amount, String description)
            throws SQLException {
        String sql = "INSERT INTO transactions(account_number, transaction_type, amount, description, transaction_time) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            statement.setString(2, type);
            statement.setBigDecimal(3, amount);
            statement.setString(4, description);
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    public List<Transaction> findByAccount(String accountNumber) throws SQLException {
        List<Transaction> transactions = new ArrayList<Transaction>();
        String sql = "SELECT transaction_id, account_number, transaction_type, amount, description, transaction_time "
                + "FROM transactions WHERE account_number = ? ORDER BY transaction_time DESC";
        try (Connection connection = JDBCUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("transaction_time");
                    transactions.add(new Transaction(
                            rs.getLong("transaction_id"),
                            rs.getString("account_number"),
                            rs.getString("transaction_type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("description"),
                            timestamp.toLocalDateTime()
                    ));
                }
            }
        }
        return transactions;
    }

    public List<Transaction> findAll() throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = " SELECT t.transaction_id, t.account_number, c.name AS customer_name, t.transaction_type, t.amount, t.description, t.transaction_time FROM transactions t JOIN accounts a ON a.account_number = t.account_number JOIN customers c ON c.customer_id = a.customer_id ORDER BY t.transaction_time DESC, t.transaction_id DESC";

        try (Connection connection = JDBCUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                transactions.add(new Transaction(
                        rs.getLong("transaction_id"),
                        rs.getString("account_number"),
                        rs.getString("customer_name"),
                        rs.getString("transaction_type"),
                        rs.getBigDecimal("amount"),
                        rs.getString("description"),
                        rs.getTimestamp("transaction_time").toLocalDateTime()
                ));
            }
        }

        return transactions;
    }
}
