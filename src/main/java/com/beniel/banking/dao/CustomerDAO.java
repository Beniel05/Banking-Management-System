package com.beniel.banking.dao;

import com.beniel.banking.model.Role;
import com.beniel.banking.util.JDBCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CustomerDAO {

    public long nextCustomerId(Connection connection) throws SQLException {

        String selectSql =
                "SELECT last_id FROM customer_id_sequence " +
                        "WHERE sequence_name = 'customer' FOR UPDATE";

        long nextId;

        try (PreparedStatement statement =
                     connection.prepareStatement(selectSql);
             ResultSet rs = statement.executeQuery()) {

            if (!rs.next()) {
                throw new SQLException(
                        "Customer ID sequence is not initialized."
                );
            }

            long lastId = rs.getLong("last_id");

            if (lastId == Long.MAX_VALUE) {
                throw new SQLException(
                        "Customer ID sequence is exhausted."
                );
            }

            nextId = lastId + 1;
        }

        String updateSql =
                "UPDATE customer_id_sequence " +
                        "SET last_id = ? WHERE sequence_name = 'customer'";

        try (PreparedStatement statement =
                     connection.prepareStatement(updateSql)) {

            statement.setLong(1, nextId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Customer ID sequence was not updated."
                );
            }
        }

        return nextId;
    }

    public void insert(
            Connection connection,
            long customerId,
            String name,
            String email,
            String passwordHash,
            Role role
    ) throws SQLException {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Customer name is required."
            );
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException(
                    "Enter a valid email."
            );
        }

        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password hash is required."
            );
        }

        if (role == null) {
            throw new IllegalArgumentException(
                    "Customer role is required."
            );
        }

        String sql =
                "INSERT INTO customers " +
                        "(customer_id, name, email, password_hash, role) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);
            statement.setString(2, name.trim());
            statement.setString(3, email.trim().toLowerCase());
            statement.setString(4, passwordHash);
            statement.setString(5, role.name());

            statement.executeUpdate();
        }
    }

    public Optional<CustomerCredentials> findCredentialsByEmail(
            String email
    ) throws SQLException {

        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql =
                "SELECT customer_id, password_hash, role " +
                        "FROM customers WHERE email = ?";

        try (Connection connection = JDBCUtil.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                String passwordHash = rs.getString("password_hash");
                String roleValue = rs.getString("role");

                if (passwordHash == null || roleValue == null) {
                    throw new SQLException(
                            "Customer credentials or role are missing."
                    );
                }

                Role role;

                try {
                    role = Role.valueOf(roleValue);
                } catch (IllegalArgumentException e) {
                    throw new SQLException(
                            "Invalid customer role stored in database.",
                            e
                    );
                }

                return Optional.of(
                        new CustomerCredentials(
                                rs.getLong("customer_id"),
                                passwordHash,
                                role
                        )
                );
            }
        }
    }

    public static final class CustomerCredentials {

        private final long customerId;
        private final String passwordHash;
        private final Role role;

        private CustomerCredentials(
                long customerId,
                String passwordHash,
                Role role
        ) {
            this.customerId = customerId;
            this.passwordHash = passwordHash;
            this.role = role;
        }

        public long getCustomerId() {
            return customerId;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public Role getRole() {
            return role;
        }
    }
}