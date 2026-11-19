package com.smartaccounting.dao;

import com.smartaccounting.db.DatabaseConnection;
import com.smartaccounting.model.JournalEntry;
import com.smartaccounting.model.JournalLine;
import com.smartaccounting.service.TransactionValidator;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class JournalDAO {

    /**
     * Saves a journal entry and all its lines atomically.
     * @param entry validated JournalEntry with lines
     * @param postImmediately true to save as POSTED, false to save as UNPOSTED
     * @return generated EntryID
     */
    public int saveJournalEntry(JournalEntry entry, boolean postImmediately) throws SQLException {
        List<String> errors = TransactionValidator.validate(entry);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid journal entry: " + String.join("; ", errors));
        }

        String insertEntrySql =
                "INSERT INTO JournalEntries (EntryDate, ReferenceNo, Description, EntryType, Status) " +
                "VALUES (?, ?, ?, ?, ?)";
        String insertLineSql =
                "INSERT INTO JournalLines (EntryID, AccountID, Debit, Credit) VALUES (?, ?, ?, ?)";

        String status = postImmediately ? "POSTED" : "UNPOSTED";
        String entryType = (entry.getEntryType() == null || entry.getEntryType().isBlank())
                ? "REGULAR" : entry.getEntryType().trim().toUpperCase();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement entryStmt = conn.prepareStatement(insertEntrySql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement lineStmt = conn.prepareStatement(insertLineSql)) {

                entryStmt.setDate(1, Date.valueOf(entry.getEntryDate()));
                entryStmt.setString(2, entry.getReferenceNo().trim());
                entryStmt.setString(3, entry.getDescription() != null ? entry.getDescription().trim() : "");
                entryStmt.setString(4, entryType);
                entryStmt.setString(5, status);
                entryStmt.executeUpdate();

                int entryId;
                try (ResultSet keys = entryStmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to retrieve generated EntryID.");
                    }
                    entryId = keys.getInt(1);
                }

                for (JournalLine line : entry.getLines()) {
                    BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
                    BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

                    lineStmt.setInt(1, entryId);
                    lineStmt.setInt(2, line.getAccountId());
                    lineStmt.setBigDecimal(3, debit);
                    lineStmt.setBigDecimal(4, credit);
                    lineStmt.addBatch();
                }

                lineStmt.executeBatch();
                conn.commit();

                entry.setEntryId(entryId);
                entry.setStatus(status);
                return entryId;

            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                throw new SQLException("Error saving journal entry: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Marks an UNPOSTED journal entry as POSTED after re-validating balance.
     */
    public boolean postJournalEntry(int entryId) throws SQLException {
        String sql = "UPDATE JournalEntries SET Status = 'POSTED' WHERE EntryID = ? AND Status = 'UNPOSTED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, entryId);
            return stmt.executeUpdate() > 0;
        }
    }
}