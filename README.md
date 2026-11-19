 SmartAccounting

A JavaFX-based desktop accounting system designed to organize and simplify essential accounting operations through a clean, elegant, and user-friendly interface.

About the Project

SmartAccounting is a desktop application developed using Java and JavaFX, with MySQL as its database management system.

It brings together essential accounting functions in one application, featuring a custom Dark Mokka interface for a more organized and comfortable user experience.

Features

- Chart of Accounts – Organizes business accounts.
- Journal Entry – Records financial transactions.
- General Journal – Displays recorded journal entries.
- General Ledger– Organizes transactions by account.
- Trial Balance – Presents account balances.
- Adjustments – Manages adjusting entries.
- Income Statement – Presents revenues and expenses.

## Technologies Used

- Java 21
- JavaFX 21
- MySQL
- JDBC
- MySQL Connector/J
- CSS

 Project Structure

```text
SmartAccounting/
├── lib/
│   └── mysql-connector-j-9.7.0.jar
├── resources/
│   └── styles/
│       └── mokka.css
├── src/
│   └── com/
│       └── smartaccounting/
│           ├── dao/
│           ├── db/
│           ├── model/
│           ├── service/
│           └── ui/
└── .gitignore
```

Requirements

- JDK 21
- JavaFX 21
- MySQL Server
- MySQL Connector/J

## Database Configuration

The application connects to a local MySQL database named `smart_accounting`.

Database connection settings are located in:

`src/com/smartaccounting/db/DatabaseConnection.java`

Make sure your MySQL server is running and the database is configured before launching the application.

## Running the Application

Compile and launch the application using your Java 21 and JavaFX 21 installation.

The main application class is:

`com.smartaccounting.ui.SmartAccountingApp`

Project Purpose

This project was developed to apply programming and accounting concepts through the creation of a functional desktop accounting application.

---

**SmartAccounting**  
*Organized accounting. Simplified management.*
