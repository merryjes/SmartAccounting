package com.smartaccounting.dao;

import com.smartaccounting.db.DatabaseConnection;
import com.smartaccounting.model.TrialBalanceRow;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrialBalanceDAO {

    public static class TrialBalanceSummary {
        private final List<TrialBalanceRow> rows;
        private final BigDecimal totalDebit;
        private final BigDecimal totalCredit;
        private final BigDecimal difference;
        private final boolean balanced;

        public TrialBalanceSummary(List<TrialBalanceRow> rows,
                                   BigDecimal totalDebit,
                                   BigDecimal totalCredit,
                                   BigDecimal difference,
                                   boolean balanced) {
            this.rows = rows;
            this.totalDebit = totalDebit;
            this.totalCredit = totalCredit;
            this.difference = difference;
            this.balanced = balanced;
        }

        public List<TrialBalanceRow> getRows() { return rows; }
        public BigDecimal getTotalDebit() { return totalDebit; }
        public BigDecimal getTotalCredit() { return totalCredit; }
        public BigDecimal getDifference() { return difference; }
        public boolean isBalanced() { return balanced; }
    }

    /**
     * Generates a Trial Balance across active accounts.
     * @param includeAdjustments false for Unadjusted Trial Balance, true for Adjusted Trial Balance
     */
    public TrialBalanceSummary getTrialBalance(boolean includeAdjustments) throws SQLException {
        String regularSubquery =
                "SELECT jl.AccountID, COALESCE(SUM(jl.Debit), 0) AS Debits, COALESCE(SUM(jl.Credit), 0) AS Credits " +
                "FROM JournalLines jl " +
                "JOIN JournalEntries je ON jl.EntryID = je.EntryID " +
                "WHERE je.Status = 'POSTED' " +
                "GROUP BY jl.AccountID";

        String combinedSubquery =
                "SELECT AccountID, SUM(Debits) AS Debits, SUM(Credits) AS Credits FROM (" +
                "  SELECT jl.AccountID, COALESCE(SUM(jl.Debit), 0) AS Debits, COALESCE(SUM(jl.Credit), 0) AS Credits " +
                "  FROM JournalLines jl " +
                "  JOIN JournalEntries je ON jl.EntryID = je.EntryID " +
                "  WHERE je.Status = 'POSTED' " +
                "  GROUP BY jl.AccountID " +
                "  UNION ALL " +
                "  SELECT al.AccountID, COALESCE(SUM(al.Debit), 0) AS Debits, COALESCE(SUM(al.Credit), 0) AS Credits " +
                "  FROM AdjustmentLines al " +
                "  JOIN Adjustments adj ON al.AdjustmentID = adj.AdjustmentID " +
                "  WHERE adj.Status = 'POSTED' " +
                "  GROUP BY al.AccountID " +
                ") combined GROUP BY AccountID";

        String totalsSource = includeAdjustments ? combinedSubquery : regularSubquery;

        String sql =
                "SELECT a.AccountID, a.AccountCode, a.AccountName, a.AccountType, " +
                "       COALESCE(t.Debits, 0) AS TotalDebits, " +
                "       COALESCE(t.Credits, 0) AS TotalCredits " +
                "FROM Accounts a " +
                "LEFT JOIN (" + totalsSource + ") t ON a.AccountID = t.AccountID " +
                "WHERE a.IsActive = TRUE " +
                "ORDER BY a.AccountCode ASC";

        List<TrialBalanceRow> rows = new ArrayList<>();
        BigDecimal grandDebit = BigDecimal.ZERO;
        BigDecimal grandCredit = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                BigDecimal debits = rs.getBigDecimal("TotalDebits");
                BigDecimal credits = rs.getBigDecimal("TotalCredits");
                BigDecimal net = debits.subtract(credits);

                BigDecimal tbDebit = BigDecimal.ZERO;
                BigDecimal tbCredit = BigDecimal.ZERO;

                if (net.compareTo(BigDecimal.ZERO) > 0) {
                    tbDebit = net;
                } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                    tbCredit = net.abs();
                }

                // Include accounts that have activity or non-zero balance
                if (debits.compareTo(BigDecimal.ZERO) != 0 || credits.compareTo(BigDecimal.ZERO) != 0) {
                    TrialBalanceRow row = new TrialBalanceRow();
                    row.setAccountId(rs.getInt("AccountID"));
                    row.setAccountCode(rs.getString("AccountCode"));
                    row.setAccountName(rs.getString("AccountName"));
                    row.setAccountType(rs.getString("AccountType"));
                    row.setDebitBalance(tbDebit);
                    row.setCreditBalance(tbCredit);
                    rows.add(row);

                    grandDebit = grandDebit.add(tbDebit);
                    grandCredit = grandCredit.add(tbCredit);
                }
            }
        }

        BigDecimal difference = grandDebit.subtract(grandCredit).abs();
        boolean balanced = grandDebit.compareTo(grandCredit) == 0;

        return new TrialBalanceSummary(rows, grandDebit, grandCredit, difference, balanced);
    }
}