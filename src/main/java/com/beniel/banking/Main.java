package com.beniel.banking;

import com.beniel.banking.dao.AccountDAO;
import com.beniel.banking.dao.CustomerDAO;
import com.beniel.banking.dao.TransactionDAO;
import com.beniel.banking.exception.AccountNotFoundException;
import com.beniel.banking.exception.InsufficientBalanceException;
import com.beniel.banking.model.BankAccount;
import com.beniel.banking.model.Role;
import com.beniel.banking.model.SavingsAccount;
import com.beniel.banking.model.Transaction;
import com.beniel.banking.service.BankService;
import com.beniel.banking.util.JDBCUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {

    private final Scanner scanner = new Scanner(System.in);
    private final BankService bankService;

    private String authenticatedAccountNumber;
    private Role authenticatedRole;

    private Main(BankService bankService) {
        this.bankService = bankService;
    }

    public static void main(String[] args) {

        try (Connection ignored = JDBCUtil.getConnection()) {

            Main app = new Main(
                    new BankService(
                            new CustomerDAO(),
                            new AccountDAO(),
                            new TransactionDAO()
                    )
            );

            app.run();

        } catch (SQLException e) {
            System.err.println(
                    "Could not connect to MySQL: " + e.getMessage()
            );

            System.err.println(
                    "Check database/schema.sql and your database connection settings."
            );
        }
    }

    private void run() {

        boolean running = true;

        while (running) {

            printMenu();

            String choice = readLine("Choose an option: ");

            try {

                switch (choice) {

                    case "1":
                        createAccount();
                        break;

                    case "2":
                        login();
                        break;

                    case "3":
                        logout();
                        break;

                    case "4":
                        viewAccount();
                        break;

                    case "5":
                        deposit();
                        break;

                    case "6":
                        withdraw();
                        break;

                    case "7":
                        transfer();
                        break;

                    case "8":
                        showTransactions();
                        break;

                    case "9":
                        showAccountSummary();
                        break;

                    case "10":
                        filterTransactions();
                        break;

                    case "11":
                        if (requireAdmin()) {
                            viewAllAccountsAsAdmin();
                        }
                        break;

                    case "12":
                        if (requireAdmin()) {
                            viewAllTransactionsAsAdmin();
                        }
                        break;

                    case "0":
                        running = false;
                        break;

                    default:
                        System.out.println(
                                "Invalid option. Enter a number from 0 to 11."
                        );
                }

            } catch (AccountNotFoundException
                     | InsufficientBalanceException e) {

                System.out.println(
                        "Operation failed: " + e.getMessage()
                );

            } catch (IllegalArgumentException e) {

                System.out.println(
                        "Invalid input: " + e.getMessage()
                );

            } catch (IllegalStateException e) {

                System.out.println(
                        "Operation failed: " + e.getMessage()
                );

            } catch (SQLException e) {

                System.out.println(
                        "Database operation failed: " + e.getMessage()
                );
            }
        }

        scanner.close();

        System.out.println(
                "Thank you for using the Banking Management System."
        );
    }

    private void printMenu() {

        System.out.println(
                "\n========== BANKING MANAGEMENT SYSTEM =========="
        );

        System.out.println("1. Create customer and account");
        System.out.println("2. Login");
        System.out.println("3. Logout");
        System.out.println("4. View account");
        System.out.println("5. Deposit");
        System.out.println("6. Withdraw");
        System.out.println("7. Transfer funds");
        System.out.println("8. View my transaction history");
        System.out.println("9. View my account summary");
        System.out.println("10. Filter transactions by type");
        System.out.println("11. View all customer accounts (Admin)");
        System.out.println("12. View all transactions (Admin)");
        System.out.println("0. Exit");
    }

    // --------------------------------------------------
    // ACCOUNT CREATION
    // --------------------------------------------------

    private void createAccount() throws SQLException {

        String name = readNonBlank(
                "Customer name: ",
                "Customer name cannot be empty."
        );

        String email = readEmail();

        char[] pin = readPin("PIN (4 digits): ");

        try {

            String type = readAccountType();

            BigDecimal openingBalance = readAmount(
                    "Opening balance (0 or more): ",
                    true
            );

            String accountNumber = bankService.createAccount(
                    name,
                    email,
                    pin,
                    type,
                    openingBalance
            );

            // Determine the role according to the configured email.
            Role role = JDBCUtil.isAdminEmail(email)
                    ? Role.ADMIN
                    : Role.CUSTOMER;

            authenticatedAccountNumber = accountNumber;
            authenticatedRole = role;

            System.out.println(
                    "Account created successfully. Account number: "
                            + accountNumber
            );

            System.out.println(
                    "Assigned role: " + authenticatedRole
            );

            System.out.println("You are now logged in.");

        } finally {
            Arrays.fill(pin, '\0');
        }
    }

    // --------------------------------------------------
    // LOGIN
    // --------------------------------------------------

    private void login() throws SQLException {

        String email = readEmail();

        char[] pin = readPin("PIN (4 digits): ");

        try {

            BankAccount account = bankService
                    .authenticate(email, pin)
                    .orElse(null);

            if (account == null) {

                System.out.println("Invalid email or PIN.");
                return;
            }

            // Always retrieve the persisted role from the database.
            Role role = bankService.getRoleByEmail(email)
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "Authenticated customer's role was not found."
                            )
                    );

            authenticatedAccountNumber =
                    account.getAccountNumber();

            authenticatedRole = role;

            System.out.println("Login successful.");
            System.out.println("Logged-in role: " + authenticatedRole);

        } finally {
            Arrays.fill(pin, '\0');
        }
    }

    // --------------------------------------------------
    // LOGOUT AND AUTHORIZATION
    // --------------------------------------------------

    private void logout() {

        if (authenticatedAccountNumber == null) {

            System.out.println("You are not logged in.");
            return;
        }

        authenticatedAccountNumber = null;
        authenticatedRole = null;

        System.out.println("Logout successful.");
    }

    private boolean requireLogin() {

        if (authenticatedAccountNumber != null) {
            return true;
        }

        System.out.println(
                "Please log in before accessing account operations."
        );

        return false;
    }

    private boolean requireAdmin() {

        if (!requireLogin()) {
            return false;
        }

        if (authenticatedRole != Role.ADMIN) {

            System.out.println(
                    "Access denied. Admin privileges required."
            );

            return false;
        }

        return true;
    }

    // --------------------------------------------------
    // VIEW ACCOUNT
    // --------------------------------------------------

    private void viewAccount() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        BankAccount account = bankService.getAccount(
                authenticatedAccountNumber
        );

        System.out.println(account);

        System.out.println(
                "Calculated interest estimate: "
                        + account.calculateInterest()
        );
    }

    // --------------------------------------------------
    // DEPOSIT
    // --------------------------------------------------

    private void deposit() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        BigDecimal amount = readAmount(
                "Deposit amount: ",
                false
        );

        bankService.deposit(
                authenticatedAccountNumber,
                amount
        );

        System.out.println("Deposit successful.");
    }

    // --------------------------------------------------
    // WITHDRAW
    // --------------------------------------------------

    private void withdraw() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        BigDecimal amount = readAmount(
                "Withdrawal amount: ",
                false
        );

        bankService.withdraw(
                authenticatedAccountNumber,
                amount
        );

        System.out.println("Withdrawal successful.");
    }

    // --------------------------------------------------
    // TRANSFER
    // --------------------------------------------------

    private void transfer() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        String destinationAccount = readNonBlank(
                "To account: ",
                "Destination account cannot be empty."
        );

        BigDecimal amount = readAmount(
                "Transfer amount: ",
                false
        );

        bankService.transfer(
                authenticatedAccountNumber,
                destinationAccount,
                amount
        );

        System.out.println("Transfer successful.");
    }

    // --------------------------------------------------
    // TRANSACTION HISTORY
    // --------------------------------------------------

    private void showTransactions() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        List<Transaction> transactions =
                bankService.getTransactions(
                        authenticatedAccountNumber
                );

        if (transactions.isEmpty()) {

            System.out.println("No transactions found.");
            return;
        }

        transactions.forEach(System.out::println);
    }

    // --------------------------------------------------
    // ACCOUNT SUMMARY
    // --------------------------------------------------

    private void showAccountSummary() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        System.out.println(
                bankService.getAccount(
                        authenticatedAccountNumber
                )
        );
    }

    // --------------------------------------------------
    // FILTER TRANSACTIONS
    // --------------------------------------------------

    private void filterTransactions() throws SQLException {

        if (!requireLogin()) {
            return;
        }

        String type;

        while (true) {

            type = readLine(
                    "Transaction type "
                            + "(DEPOSIT/WITHDRAWAL/TRANSFER_IN/TRANSFER_OUT): "
            ).toUpperCase();

            if (Arrays.asList(
                    "DEPOSIT",
                    "WITHDRAWAL",
                    "TRANSFER_IN",
                    "TRANSFER_OUT"
            ).contains(type)) {

                break;
            }

            System.out.println(
                    "Invalid transaction type. "
                            + "Please choose one of the listed types."
            );
        }

        final String selectedType = type;

        List<Transaction> filtered = bankService
                .getTransactions(authenticatedAccountNumber)
                .stream()
                .filter(t -> t.getType().equals(selectedType))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {

            System.out.println(
                    "No transactions found for type "
                            + selectedType + "."
            );

            return;
        }

        filtered.forEach(System.out::println);
    }

    // --------------------------------------------------
    // ADMIN: VIEW ALL ACCOUNTS
    // --------------------------------------------------

    private void viewAllAccountsAsAdmin() throws SQLException {

        // The caller has already checked requireAdmin().

        List<BankAccount> accounts =
                bankService.getAllAccounts();

        if (accounts.isEmpty()) {

            System.out.println("No customer accounts found.");
            return;
        }

        System.out.println(
                "\n================ ALL CUSTOMER ACCOUNTS ================"
        );

        System.out.printf(
                "%-22s %-24s %-12s %15s%n",
                "ACCOUNT NUMBER",
                "CUSTOMER NAME",
                "TYPE",
                "BALANCE"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        BigDecimal totalBalance = BigDecimal.ZERO;

        for (BankAccount account : accounts) {

            String type = account instanceof SavingsAccount
                    ? "SAVINGS"
                    : "CURRENT";

            System.out.printf(
                    "%-22s %-24s %-12s ₹%,12.2f%n",
                    account.getAccountNumber(),
                    truncate(account.getCustomerName(), 24),
                    type,
                    account.getBalance()
            );

            totalBalance = totalBalance.add(
                    account.getBalance()
            );
        }

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        System.out.println(
                "Total accounts: " + accounts.size()
        );

        System.out.printf(
                "Combined account balances: ₹%,.2f%n",
                totalBalance
        );

        System.out.println(
                "Note: Combined balances are a display total, "
                        + "not bank-owned funds."
        );
    }

    private void viewAllTransactionsAsAdmin() throws SQLException {
        List<Transaction> transactions = bankService.getAllTransactions();

        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.println("\n================ ALL TRANSACTIONS ================");

        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
    }

    private String truncate(String value, int maxLength) {

        if (value == null) {
            return "";
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength - 3) + "...";
    }

    // --------------------------------------------------
    // INPUT HELPERS
    // --------------------------------------------------

    private String readLine(String prompt) {

        System.out.print(prompt);

        return scanner.nextLine().trim();
    }

    private String readNonBlank(
            String prompt,
            String errorMessage
    ) {

        while (true) {

            String value = readLine(prompt);

            if (!value.isEmpty()) {
                return value;
            }

            System.out.println(
                    "Invalid input: " + errorMessage
            );
        }
    }

    private String readEmail() {

        while (true) {

            String email = readLine("Customer email: ");

            if (email.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            )) {

                return email;
            }

            System.out.println(
                    "Invalid email format. "
                            + "Please enter an email such as name@example.com."
            );
        }
    }

    private char[] readPin(String prompt) {

        while (true) {

            char[] pin = readLine(prompt).toCharArray();

            boolean valid = pin.length == 4;

            if (valid) {

                for (char digit : pin) {

                    if (digit < '0' || digit > '9') {

                        valid = false;
                        break;
                    }
                }
            }

            if (valid) {
                return pin;
            }

            Arrays.fill(pin, '\0');

            System.out.println(
                    "Invalid PIN. Enter exactly 4 digits (0-9)."
            );
        }
    }

    private String readAccountType() {

        while (true) {

            String type = readLine(
                    "Account type (SAVINGS/CURRENT): "
            ).toUpperCase();

            if (type.equals("SAVINGS")
                    || type.equals("CURRENT")) {

                return type;
            }

            System.out.println(
                    "Invalid account type. Enter SAVINGS or CURRENT."
            );
        }
    }

    private BigDecimal readAmount(
            String prompt,
            boolean allowZero
    ) {

        while (true) {

            String value = readLine(prompt);

            try {

                BigDecimal amount = new BigDecimal(value);

                if (amount.scale() > 2) {

                    System.out.println(
                            "Invalid input: Amounts may have "
                                    + "at most two decimal places."
                    );

                    continue;
                }

                int comparison = amount.compareTo(
                        BigDecimal.ZERO
                );

                if (comparison < 0
                        || (!allowZero && comparison == 0)) {

                    System.out.println(
                            allowZero
                                    ? "Invalid input: Opening balance cannot be negative."
                                    : "Invalid input: Amount must be greater than zero."
                    );

                    continue;
                }

                return amount;

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input: Enter a valid numeric amount."
                );
            }
        }
    }
}