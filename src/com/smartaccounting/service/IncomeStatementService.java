package com.smartaccounting.service;

import com.smartaccounting.dao.TrialBalanceDAO;
import com.smartaccounting.model.TrialBalanceRow;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IncomeStatementService {

    public static class IncomeStatementReport {
        private final String businessName;
        private final String accountingPeriod;
        private final Map<String, BigDecimal> revenues;
        private final Map<String, BigDecimal> expenses;
        private final BigDecimal totalRevenue;
        private final BigDecimal totalExpenses;
        private final BigDecimal netAmount; // positive = Net Income, negative = Net Loss

        public IncomeStatementReport(String businessName,
                                     String accountingPeriod,
                                     Map<String, BigDecimal> revenues,
                                     Map<String, BigDecimal> expenses,
                                     BigDecimal totalRevenue,
                                     BigDecimal totalExpenses,
                                     BigDecimal netAmount) {
            this.businessName = businessName;
            this.accountingPeriod = accountingPeriod;
            this.revenues = revenues;
            this.expenses = expenses;
            this.totalRevenue = totalRevenue;
            this.totalExpenses = totalExpenses;
            this.netAmount = netAmount;
        }

        public String getBusinessName() { return businessName; }
        public String getAccountingPeriod() { return accountingPeriod; }
        public Map<String, BigDecimal> getRevenues() { return revenues; }
        public Map<String, BigDecimal> getExpenses() { return expenses; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getNetAmount() { return netAmount; }
        public boolean isNetIncome() { return netAmount.compareTo(BigDecimal.ZERO) >= 0; }
    }

    public IncomeStatementReport generateAdjustedIncomeStatement(String businessName, String accountingPeriod)
            throws SQLException {
        TrialBalanceDAO tbDao = new TrialBalanceDAO();
        TrialBalanceDAO.TrialBalanceSummary adjustedTb = tbDao.getTrialBalance(true);

        Map<String, BigDecimal> revenues = new LinkedHashMap<>();
        Map<String, BigDecimal> expenses = new LinkedHashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (TrialBalanceRow row : adjustedTb.getRows()) {
            String label = row.getAccountCode() + " - " + row.getAccountName();

            if ("REVENUE".equalsIgnoreCase(row.getAccountType())) {
                BigDecimal amount = row.getCreditBalance().subtract(row.getDebitBalance());
                revenues.put(label, amount);
                totalRevenue = totalRevenue.add(amount);
            } else if ("EXPENSE".equalsIgnoreCase(row.getAccountType())) {
                BigDecimal amount = row.getDebitBalance().subtract(row.getCreditBalance());
                expenses.put(label, amount);
                totalExpenses = totalExpenses.add(amount);
            }
        }

        BigDecimal netAmount = totalRevenue.subtract(totalExpenses);

        return new IncomeStatementReport(
                businessName,
                accountingPeriod,
                revenues,
                expenses,
                totalRevenue,
                totalExpenses,
                netAmount
        );
    }

    public void exportToCsv(String filePath, IncomeStatementReport report) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath))) {
            out.println("Business Name,\"" + escapeCsv(report.getBusinessName()) + "\"");
            out.println("Report Title,Income Statement");
            out.println("Accounting Period,\"" + escapeCsv(report.getAccountingPeriod()) + "\"");
            out.println("Generated On," + LocalDate.now());
            out.println();

            out.println("REVENUE,Amount");
            for (Map.Entry<String, BigDecimal> entry : report.getRevenues().entrySet()) {
                out.printf("\"%s\",%.2f%n", escapeCsv(entry.getKey()), entry.getValue());
            }
            out.printf("Total Revenue,%.2f%n%n", report.getTotalRevenue());

            out.println("EXPENSES,Amount");
            for (Map.Entry<String, BigDecimal> entry : report.getExpenses().entrySet()) {
                out.printf("\"%s\",%.2f%n", escapeCsv(entry.getKey()), entry.getValue());
            }
            out.printf("Total Expenses,%.2f%n%n", report.getTotalExpenses());

            String netLabel = report.isNetIncome() ? "Net Income" : "Net Loss";
            out.printf("%s,%.2f%n", netLabel, report.getNetAmount().abs());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}