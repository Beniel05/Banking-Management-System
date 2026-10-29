# Banking Management System

**Current version:** `1.0.0`  
**Technology:** Java 21, Maven, JDBC, and MySQL

A menu-driven console project demonstrating Java, OOP, Collections, custom exceptions, and JDBC persistence.

## Recommended setup: Maven (avoids manual JDBC classpath errors)

Requirements:
- JDK 21 or later
- Maven 3.6+
- MySQL 8.0.16 or later

1. Open MySQL Workbench and execute `database/schema.sql`.
2. Configure the connection settings. The current `JDBCUtil` defaults to user `root` and password `root`; change these defaults or, preferably, set environment variables in the same terminal used to run the application.
3. Example for Windows PowerShell:

```powershell
$env:BANK_DB_URL="jdbc:mysql://localhost:3306/banking_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:BANK_DB_USER="root"
$env:BANK_DB_PASSWORD="your_mysql_password"
```

4. From the project root, run:
```bash
mvn clean compile
mvn exec:java
```

The Maven `pom.xml` downloads MySQL Connector/J automatically and places it on the runtime classpath. This is the recommended way to avoid `No suitable driver found`.

If using IntelliJ IDEA or Eclipse, open/import the project as an existing Maven project and wait for dependencies to download, then run `com.beniel.banking.Main`.

## Manual setup (without Maven)

1. Download MySQL Connector/J from the official MySQL download page:
   https://dev.mysql.com/downloads/connector/j/
2. Put the driver JAR in `lib/` and name it `mysql-connector-j.jar`.
3. Compile and run with the JAR on the compile-time and runtime classpaths. The exact commands depend on your shell or IDE; Maven is recommended to manage this automatically.

## Features
- Create customers and savings/current accounts with a four-digit numeric PIN
- Store PINs as salted PBKDF2 hashes; authenticate before account operations
- Deposit, withdraw, transfer funds
- View account and transaction history
- Admin-only listing of customer accounts and transaction history
- Re-prompt for invalid input fields instead of aborting the whole operation
- Filter transactions by type with Streams
- Authenticate before accessing a customer's account
- Uses encapsulation, inheritance, abstraction, runtime polymorphism
- Uses `Predicate`, `Optional`, `LocalDateTime`, Streams, Lambdas, method references
- JDBC DAO layer with prepared statements
- Atomic transfer using a database transaction
- Transactional customer ID allocation, so failed account creation does not consume an ID
- Custom exceptions and basic input validation


## Administrator access
Options `11` and `12` list customer accounts and transactions for an authenticated administrator. The current code assigns the `ADMIN` role to the email `admin@gmail.com` (defined in `JDBCUtil`) and stores that role in the database. This is a demonstration-only setup, not a secure administrator provisioning process; change it before using real or shared data.

## Important limitations
This is an educational demo, not production banking software. It does not implement production-grade authorization, concurrency-safe account locking, regulatory compliance, or financial controls. Four-digit PINs are salted and hashed, but the small PIN space and demo authorization controls are not suitable for a production banking system. Amounts are represented with `BigDecimal`, but production systems need additional safeguards and tests.

`database/schema.sql` describes the current schema, including customer roles, hashed PINs, and the customer ID sequence. `CREATE TABLE IF NOT EXISTS` does not migrate tables from older versions. Back up existing data and plan an appropriate schema/data migration before upgrading an older database; existing customer records may need credentials and roles populated before they can log in.
