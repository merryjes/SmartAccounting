package com.smartaccounting.ui;

import com.smartaccounting.dao.*;
import com.smartaccounting.model.*;
import com.smartaccounting.service.IncomeStatementService;
import com.smartaccounting.service.TransactionValidator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("       SMART ACCOUNTING SYSTEM (CLI)      ");
            System.out.println("==========================================");
            System.out.println("1. Record Journal Entry (Module 2)");
            System.out.println("2. View General Journal (Module 3)");
            System.out.println("3. View General Ledger (Module 4)");
            System.out.println("4. View Unadjusted Trial Balance (Module 5)");
            System.out.println("5. Record Adjusting Entry (Module 6)");
            System.out.println("6. View Adjusted Trial Balance (Module 7)");
            System.out.println("7. Generate Income Statement (Module 8)");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> recordJournalEntry();
                    case "2" -> viewGeneralJournal();
                    case "3" -> viewGeneralLedger();
                    case "4" -> viewTrialBalance(false, "UNADJUSTED TRIAL BALANCE");
                    case "5" -> recordAdjustment();
                    case "6" -> viewTrialBalance(true, "ADJUSTED TRIAL BALANCE");
                    case "7" -> viewIncomeStatement();
                    case "0" -> {
                        System.out.println("Exiting Smart Accounting System. Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void recordJournalEntry() throws SQLException {
        System.out.print("Date (YYYY-MM-DD): ");
        LocalDate date = LocalDate.parse(scanner.nextLine().trim());

        System.out.print("Reference No (e.g., JV-001): ");
        String refNo = scanner.nextLine().trim();

        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();

        JournalEntry entry = new JournalEntry(date, refNo, desc);

        System.out.print("How many lines? ");
        int lineCount = Integer.parseInt(scanner.nextLine().trim());

        for (int i = 1; i <= lineCount; i++) {
            System.out.println("--- Line " + i + " ---");
            System.out.print("Account ID: ");
            int accId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Debit amount (0 if none): ");
            BigDecimal debit = new BigDecimal(scanner.nextLine().trim());

            System.out.print("Credit amount (0 if none): ");
            BigDecimal credit = new BigDecimal(scanner.nextLine().trim());

            JournalLine line = new JournalLine();
            line.setAccountId(accId);
            line.setDebit(debit);
            line.setCredit(credit);
            entry.addLine(line);
        }

        // Preview & validate before saving
        System.out.println("\n--- ENTRY PREVIEW ---");
        System.out.println("Date: " + entry.getEntryDate() + " | Ref: " + entry.getReferenceNo());
        System.out.println("Desc: " + entry.getDescription());
        System.out.println("Total Debit: " + entry.getTotalDebit() + " | Total Credit: " + entry.getTotalCredit());

        List<String> errors = TransactionValidator.validate(entry);
        if (!errors.isEmpty()) {
            System.out.println("Validation failed:");
            errors.forEach(err -> System.out.println("  - " + err));
            return;
        }

        System.out.print("Post immediately? (y/n): ");
        boolean postNow = scanner.nextLine().trim().equalsIgnoreCase("y");

        JournalDAO dao = new JournalDAO();
        int id = dao.saveJournalEntry(entry, postNow);
        System.out.println("Saved Journal Entry #" + id + " with status: " + entry.getStatus());
    }

    private static void viewGeneralJournal() throws SQLException {
        GeneralJournalDAO dao = new GeneralJournalDAO();
        List<JournalEntry> entries = dao.getGeneralJournal(false);

        System.out.println("\n=== GENERAL JOURNAL ===");
        for (JournalEntry je : entries) {
            System.out.printf("[%s] %s | Ref: %s | %s (%s)%n",
                    je.getStatus(), je.getEntryDate(), je.getReferenceNo(), je.getDescription(), je.getEntryType());
            for (JournalLine jl : je.getLines()) {
                System.out.printf("   %-10s %-28s Dr: %10.2f   Cr: %10.2f%n",
                        jl.getAccountCode(), jl.getAccountName(), jl.getDebit(), jl.getCredit());
            }
        }
    }

    private static void viewGeneralLedger() throws SQLException {
        System.out.print("Enter Account ID: ");
        int accountId = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Include posted adjusting entries? (y/n): ");
        boolean includeAdj = scanner.nextLine().trim().equalsIgnoreCase("y");

        GeneralLedgerDAO dao = new GeneralLedgerDAO();
        List<LedgerEntryRow> rows = dao.getLedgerForAccount(accountId, includeAdj);

        System.out.println("\n=== GENERAL LEDGER (Account ID: " + accountId + ") ===");
        for (LedgerEntryRow r : rows) {
            System.out.printf("%s | %-8s | %-10s | %-22s | Dr: %9.2f | Cr: %9.2f | Bal: %10.2f%n",
                    r.getDate(), r.getSourceType(), r.getReferenceNo(), r.getDescription(),
                    r.getDebit(), r.getCredit(), r.getRunningBalance());
        }
    }

    private static void viewTrialBalance(boolean includeAdjustments, String title) throws SQLException {
        TrialBalanceDAO dao = new TrialBalanceDAO();
        TrialBalanceDAO.TrialBalanceSummary summary = dao.getTrialBalance(includeAdjustments);

        System.out.println("\n=== " + title + " ===");
        for (TrialBalanceRow row : summary.getRows()) {
            System.out.printf("%-10s %-28s %-10s Dr: %10.2f   Cr: %10.2f%n",
                    row.getAccountCode(), row.getAccountName(), row.getAccountType(),
                    row.getDebitBalance(), row.getCreditBalance());
        }
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("Total Debit:  %10.2f%n", summary.getTotalDebit());
        System.out.printf("Total Credit: %10.2f%n", summary.getTotalCredit());
        System.out.printf("Difference:   %10.2f | Status: %s%n",
                summary.getDifference(), summary.isBalanced() ? "BALANCED" : "NOT BALANCED");
    }

    private static void recordAdjustment() throws SQLException {
        AdjustmentEntry adj = new AdjustmentEntry();

        System.out.print("Adjustment Date (YYYY-MM-DD): ");
        adj.setAdjustmentDate(LocalDate.parse(scanner.nextLine().trim()));

        System.out.print("Reference No (e.g., ADJ-001): ");
        adj.setReferenceNo(scanner.nextLine().trim());

        System.out.print("Debit Account ID: ");
        adj.setDebitAccountId(Integer.parseInt(scanner.nextLine().trim()));

        System.out.print("Credit Account ID: ");
        adj.setCreditAccountId(Integer.parseInt(scanner.nextLine().trim()));

        System.out.print("Amount: ");
        adj.setAmount(new BigDecimal(scanner.nextLine().trim()));

        System.out.print("Explanation / Reason: ");
        adj.setDescription(scanner.nextLine().trim());

        System.out.print("Post immediately? (y/n): ");
        boolean postNow = scanner.nextLine().trim().equalsIgnoreCase("y");

        AdjustmentDAO dao = new AdjustmentDAO();
        int id = dao.saveAdjustment(adj, postNow);
        System.out.println("Saved Adjusting Entry #" + id + " with status: " + adj.getStatus());
    }

    private static void viewIncomeStatement() throws SQLException {
        System.out.print("Business Name: ");
        String bizName = scanner.nextLine().trim();

        System.out.print("Accounting Period (e.g., For the Month Ended Nov 2026): ");
        String period = scanner.nextLine().trim();

        IncomeStatementService service = new IncomeStatementService();
        IncomeStatementService.IncomeStatementReport report =
                service.generateAdjustedIncomeStatement(bizName, period);

        System.out.println("\n==========================================");
        System.out.println(report.getBusinessName());
        System.out.println("Income Statement");
        System.out.println(report.getAccountingPeriod());
        System.out.println("==========================================");

        System.out.println("REVENUE");
        for (Map.Entry<String, BigDecimal> e : report.getRevenues().entrySet()) {
            System.out.printf("  %-35s %10.2f%n", e.getKey(), e.getValue());
        }
        System.out.printf("Total Revenue:                      %10.2f%n%n", report.getTotalRevenue());

        System.out.println("EXPENSES");
        for (Map.Entry<String, BigDecimal> e : report.getExpenses().entrySet()) {
            System.out.printf("  %-35s %10.2f%n", e.getKey(), e.getValue());
        }
        System.out.printf("Total Expenses:                     %10.2f%n%n", report.getTotalExpenses());

        String netLabel = report.isNetIncome() ? "Net Income:" : "Net Loss:";
        System.out.printf("%-37s %10.2f%n", netLabel, report.getNetAmount().abs());
    }
}