package com.smartaccounting.dao;

import com.smartaccounting.db.DatabaseConnection;
import com.smartaccounting.model.AdjustmentEntry;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdjustmentDAO {

    public int saveAdjustment(AdjustmentEntry adj, boolean postImmediately) throws SQLException {
        if (adj == null) {
            throw new IllegalArgumentException("Adjustment entry cannot be null.");
        }
        if (adj.getAdjustmentDate() == null) {
            throw new IllegalArgumentException("Adjustment date is required.");
        }
        if (adj.getReferenceNo() == null || adj.getReferenceNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Adjustment reference number is required.");
        }
        if (adj.getDescription() == null || adj.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Explanation / reason for adjustment is required.");
        }
        if (adj.getDebitAccountId() <= 0 || adj.getCreditAccountId() <= 0) {
            throw new IllegalArgumentException("Both debit and credit accounts must be selected.");
        }
        if (adj.getDebitAccountId() == adj.getCreditAccountId()) {
            throw new IllegalArgumentException("Debit account and credit account must be different.");
        }
        if (adj.getAmount() == null || adj.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Adjustment amount must be greater than zero.");
        }

        String insertHeaderSql =
                "INSERT INTO Adjustments (AdjustmentDate, ReferenceNo, Description, Status) " +
                "VALUES (?, ?, ?, ?)";
        String insertLineSql =
                "INSERT INTO AdjustmentLines (AdjustmentID, AccountID, Debit, Credit) VALUES (?, ?, ?, ?)";

        String status = postImmediately ? "POSTED" : "UNPOSTED";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement headerStmt = conn.prepareStatement(insertHeaderSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement lineStmt = conn.prepareStatement(insertLineSql)) {

                headerStmt.setDate(1, Date.valueOf(adj.getAdjustmentDate()));
                headerStmt.setString(2, adj.getReferenceNo().trim());
                headerStmt.setString(3, adj.getDescription().trim());
                headerStmt.setString(4, status);
                headerStmt.executeUpdate();

                int adjId;
                try (ResultSet keys = headerStmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to retrieve generated AdjustmentID.");
                    }
                    adjId = keys.getInt(1);
                }

                // Debit line
                lineStmt.setInt(1, adjId);
                lineStmt.setInt(2, adj.getDebitAccountId());
                lineStmt.setBigDecimal(3, adj.getAmount());
                lineStmt.setBigDecimal(4, BigDecimal.ZERO);
                lineStmt.addBatch();

                // Credit line
                lineStmt.setInt(1, adjId);
                lineStmt.setInt(2, adj.getCreditAccountId());
                lineStmt.setBigDecimal(3, BigDecimal.ZERO);
                lineStmt.setBigDecimal(4, adj.getAmount());
                lineStmt.addBatch();

                lineStmt.executeBatch();
                conn.commit();

                adj.setAdjustmentId(adjId);
                adj.setStatus(status);
                return adjId;

            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                throw new SQLException("Error saving adjustment entry: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<AdjustmentEntry> getAllAdjustments() throws SQLException {
        String sql =
                "SELECT adj.AdjustmentID, adj.AdjustmentDate, adj.ReferenceNo, adj.Description, adj.Status, " +
                "       dl.AccountID AS DebitAccId, da.AccountCode AS DebitCode, da.AccountName AS DebitName, dl.Debit AS Amount, " +
                "       cl.AccountID AS CreditAccId, ca.AccountCode AS CreditCode, ca.AccountName AS CreditName " +
                "FROM Adjustments adj " +
                "JOIN AdjustmentLines dl ON adj.AdjustmentID = dl.AdjustmentID AND dl.Debit > 0 " +
                "JOIN AdjustmentLines cl ON adj.AdjustmentID = cl.AdjustmentID AND cl.Credit > 0 " +
                "JOIN Accounts da ON dl.AccountID = da.AccountID " +
                "JOIN Accounts ca ON cl.AccountID = ca.AccountID " +
                "ORDER BY adj.AdjustmentDate ASC, adj.AdjustmentID ASC";

        List<AdjustmentEntry> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                AdjustmentEntry entry = new AdjustmentEntry();
                entry.setAdjustmentId(rs.getInt("AdjustmentID"));
                entry.setAdjustmentDate(rs.getDate("AdjustmentDate").toLocalDate());
                entry.setReferenceNo(rs.getString("ReferenceNo"));
                entry.setDescription(rs.getString("Description"));
                entry.setStatus(rs.getString("Status"));
                entry.setDebitAccountId(rs.getInt("DebitAccId"));
                entry.setDebitAccountCode(rs.getString("DebitCode"));
                entry.setDebitAccountName(rs.getString("DebitName"));
                entry.setCreditAccountId(rs.getInt("CreditAccId"));
                entry.setCreditAccountCode(rs.getString("CreditCode"));
                entry.setCreditAccountName(rs.getString("CreditName"));
                entry.setAmount(rs.getBigDecimal("Amount"));
                list.add(entry);
            }
        }
        return list;
    }

    public boolean postAdjustment(int adjustmentId) throws SQLException {
        String sql = "UPDATE Adjustments SET Status = 'POSTED' WHERE AdjustmentID = ? AND Status = 'UNPOSTED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adjustmentId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteUnpostedAdjustment(int adjustmentId) throws SQLException {
        String sql = "DELETE FROM Adjustments WHERE AdjustmentID = ? AND Status = 'UNPOSTED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adjustmentId);
            return stmt.executeUpdate() > 0;
        }
    }
}