package com.smartaccounting.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalEntry {
    private int entryId;
    private LocalDate entryDate;
    private String referenceNo;
    private String description;
    private String entryType;
    private String status;
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry() {
        this.entryType = "REGULAR";
        this.status = "UNPOSTED";
    }

    public JournalEntry(LocalDate entryDate, String referenceNo, String description) {
        this();
        this.entryDate = entryDate;
        this.referenceNo = referenceNo;
        this.description = description;
    }

    public void addLine(JournalLine line) {
        this.lines.add(line);
    }

    public BigDecimal getTotalDebit() {
        BigDecimal total = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            if (line.getDebit() != null) {
                total = total.add(line.getDebit());
            }
        }
        return total;
    }

    public BigDecimal getTotalCredit() {
        BigDecimal total = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            if (line.getCredit() != null) {
                total = total.add(line.getCredit());
            }
        }
        return total;
    }

    public boolean isBalanced() {
        return getTotalDebit().compareTo(getTotalCredit()) == 0;
    }

    public int getEntryId() { return entryId; }
    public void setEntryId(int entryId) { this.entryId = entryId; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<JournalLine> getLines() { return lines; }
    public void setLines(List<JournalLine> lines) { this.lines = lines; }
}