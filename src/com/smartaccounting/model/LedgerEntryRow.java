package com.smartaccounting.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LedgerEntryRow {
    private LocalDate date;
    private String description;
    private String referenceNo;
    private String sourceType; // REGULAR or ADJUSTING
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;

    public LedgerEntryRow() {
        this.debit = BigDecimal.ZERO;
        this.credit = BigDecimal.ZERO;
        this.runningBalance = BigDecimal.ZERO;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }
}