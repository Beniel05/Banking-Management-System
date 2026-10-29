package com.beniel.banking.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class JDBCUtil {

    // FIXED: Appended &allowPublicKeyRetrieval=true to bypass the authentication security error
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/banking_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private JDBCUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("BANK_DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("BANK_DB_USER", "root");
        String password = System.getenv().getOrDefault("BANK_DB_PASSWORD", "root");

        return DriverManager.getConnection(url, user, password);
    }

    private static final String ADMIN_EMAIL = "admin@gmail.com";

    public static boolean isAdminEmail(String email) {
        return ADMIN_EMAIL.equalsIgnoreCase(email.trim());
    }
}
