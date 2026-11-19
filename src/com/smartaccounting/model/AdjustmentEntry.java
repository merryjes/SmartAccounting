package com.smartaccounting.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdjustmentEntry {
    private int adjustmentId;
    private LocalDate adjustmentDate;
    private String referenceNo;
    private String description;
    private String status; // UNPOSTED or POSTED

    private int debitAccountId;
    private String debitAccountCode;
    private String debitAccountName;

    private int creditAccountId;
    private String creditAccountCode;
    private String creditAccountName;

    private BigDecimal amount;

    public AdjustmentEntry() {
        this.status = "UNPOSTED";
        this.amount = BigDecimal.ZERO;
    }

    public int getAdjustmentId() { return adjustmentId; }
    public void setAdjustmentId(int adjustmentId) { this.adjustmentId = adjustmentId; }

    public LocalDate getAdjustmentDate() { return adjustmentDate; }
    public void setAdjustmentDate(LocalDate adjustmentDate) { this.adjustmentDate = adjustmentDate; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDebitAccountId() { return debitAccountId; }
    public void setDebitAccountId(int debitAccountId) { this.debitAccountId = debitAccountId; }

    public String getDebitAccountCode() { return debitAccountCode; }
    public void setDebitAccountCode(String debitAccountCode) { this.debitAccountCode = debitAccountCode; }

    public String getDebitAccountName() { return debitAccountName; }
    public void setDebitAccountName(String debitAccountName) { this.debitAccountName = debitAccountName; }

    public int getCreditAccountId() { return creditAccountId; }
    public void setCreditAccountId(int creditAccountId) { this.creditAccountId = creditAccountId; }

    public String getCreditAccountCode() { return creditAccountCode; }
    public void setCreditAccountCode(String creditAccountCode) { this.creditAccountCode = creditAccountCode; }

    public String getCreditAccountName() { return creditAccountName; }
    public void setCreditAccountName(String creditAccountName) { this.creditAccountName = creditAccountName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}