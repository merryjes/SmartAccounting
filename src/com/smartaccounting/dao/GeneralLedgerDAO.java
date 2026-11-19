package com.smartaccounting.dao;

import com.smartaccounting.db.DatabaseConnection;
import com.smartaccounting.model.LedgerEntryRow;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GeneralLedgerDAO {

    /**
     * Generates the ledger rows with running balance for a single account.
     * @param accountId target account ID
     * @param includeAdjustments false for Unadjusted Ledger, true for Adjusted Ledger
     */
    public List<LedgerEntryRow> getLedgerForAccount(int accountId, boolean includeAdjustments) throws SQLException {
        String normalBalance = getNormalBalance(accountId);

        String baseSql =
                "SELECT je.EntryDate AS TxDate, je.Description, je.ReferenceNo, 'REGULAR' AS SourceType, " +
                "       jl.Debit, jl.Credit, 1 AS SortOrder, jl.LineID AS RowId " +
                "FROM JournalEntries je " +
                "JOIN JournalLines jl ON je.EntryID = jl.EntryID " +
                "WHERE je.Status = 'POSTED' AND jl.AccountID = ? ";

        String adjSql =
                "UNION ALL " +
                "SELECT adj.AdjustmentDate AS TxDate, adj.Description, adj.ReferenceNo, 'ADJUSTING' AS SourceType, " +
                "       al.Debit, al.Credit, 2 AS SortOrder, al.LineID AS RowId " +
                "FROM Adjustments adj " +
                "JOIN AdjustmentLines al ON adj.AdjustmentID = al.AdjustmentID " +
                "WHERE adj.Status = 'POSTED' AND al.AccountID = ? ";

        String finalSql = (includeAdjustments ? (baseSql + adjSql) : baseSql) +
                          "ORDER BY TxDate ASC, SortOrder ASC, RowId ASC";

        List<LedgerEntryRow> rows = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(finalSql)) {

            stmt.setInt(1, accountId);
            if (includeAdjustments) {
                stmt.setInt(2, accountId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal debit = rs.getBigDecimal("Debit") != null
                            ? rs.getBigDecimal("Debit") : BigDecimal.ZERO;
                    BigDecimal credit = rs.getBigDecimal("Credit") != null
                            ? rs.getBigDecimal("Credit") : BigDecimal.ZERO;

                    if ("CREDIT".equalsIgnoreCase(normalBalance)) {
                        runningBalance = runningBalance.add(credit).subtract(debit);
                    } else {
                        // Default DEBIT normal balance: +Debit - Credit
                        runningBalance = runningBalance.add(debit).subtract(credit);
                    }

                    LedgerEntryRow row = new LedgerEntryRow();
                    row.setDate(rs.getDate("TxDate").toLocalDate());
                    row.setDescription(rs.getString("Description"));
                    row.setReferenceNo(rs.getString("ReferenceNo"));
                    row.setSourceType(rs.getString("SourceType"));
                    row.setDebit(debit);
                    row.setCredit(credit);
                    row.setRunningBalance(runningBalance);
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private String getNormalBalance(int accountId) throws SQLException {
        String sql = "SELECT NormalBalance FROM Accounts WHERE AccountID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("NormalBalance");
                }
            }
        }
        throw new SQLException("Account not found with ID: " + accountId);
    }
}