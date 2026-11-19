package com.smartaccounting.dao;

import com.smartaccounting.db.DatabaseConnection;
import com.smartaccounting.model.JournalEntry;
import com.smartaccounting.model.JournalLine;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeneralJournalDAO {

    /**
     * Fetches journal entries chronologically with their debit/credit lines.
     * @param postedOnly if true, returns only POSTED entries; if false, returns all entries.
     */
    public List<JournalEntry> getGeneralJournal(boolean postedOnly) throws SQLException {
        String sql =
                "SELECT je.EntryID, je.EntryDate, je.ReferenceNo, je.Description, je.EntryType, je.Status, " +
                "       jl.LineID, jl.AccountID, a.AccountCode, a.AccountName, jl.Debit, jl.Credit " +
                "FROM JournalEntries je " +
                "JOIN JournalLines jl ON je.EntryID = jl.EntryID " +
                "JOIN Accounts a ON jl.AccountID = a.AccountID " +
                (postedOnly ? "WHERE je.Status = 'POSTED' " : "") +
                "ORDER BY je.EntryDate ASC, je.EntryID ASC, jl.LineID ASC";

        Map<Integer, JournalEntry> entryMap = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int entryId = rs.getInt("EntryID");
                JournalEntry entry = entryMap.get(entryId);
                if (entry == null) {
                    entry = new JournalEntry();
                    entry.setEntryId(entryId);
                    entry.setEntryDate(rs.getDate("EntryDate").toLocalDate());
                    entry.setReferenceNo(rs.getString("ReferenceNo"));
                    entry.setDescription(rs.getString("Description"));
                    entry.setEntryType(rs.getString("EntryType"));
                    entry.setStatus(rs.getString("Status"));
                    entryMap.put(entryId, entry);
                }

                JournalLine line = new JournalLine();
                line.setLineId(rs.getInt("LineID"));
                line.setEntryId(entryId);
                line.setAccountId(rs.getInt("AccountID"));
                line.setAccountCode(rs.getString("AccountCode"));
                line.setAccountName(rs.getString("AccountName"));
                line.setDebit(rs.getBigDecimal("Debit"));
                line.setCredit(rs.getBigDecimal("Credit"));
                entry.addLine(line);
            }
        }

        return new ArrayList<>(entryMap.values());
    }

    /**
     * Exports the General Journal to a CSV file (e.g., general_journal.csv).
     */
    public void exportToCsv(String filePath, boolean postedOnly) throws SQLException, IOException {
        List<JournalEntry> entries = getGeneralJournal(postedOnly);

        try (PrintWriter out = new PrintWriter(new FileWriter(filePath))) {
            out.println("Date,Reference No,Account Code,Account Name,Description,Status,Debit,Credit");

            for (JournalEntry entry : entries) {
                for (JournalLine line : entry.getLines()) {
                    BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
                    BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

                    out.printf("%s,%s,%s,\"%s\",\"%s\",%s,%.2f,%.2f%n",
                            escapeCsv(entry.getEntryDate().toString()),
                            escapeCsv(entry.getReferenceNo()),
                            escapeCsv(line.getAccountCode()),
                            escapeCsv(line.getAccountName()),
                            escapeCsv(entry.getDescription()),
                            escapeCsv(entry.getStatus()),
                            debit,
                            credit);
                }
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}