package com.smartaccounting.model;

import java.math.BigDecimal;

public class TrialBalanceRow {
    private int accountId;
    private String accountCode;
    private String accountName;
    private String accountType;
    private BigDecimal debitBalance;
    private BigDecimal creditBalance;

    public TrialBalanceRow() {
        this.debitBalance = BigDecimal.ZERO;
        this.creditBalance = BigDecimal.ZERO;
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getDebitBalance() { return debitBalance; }
    public void setDebitBalance(BigDecimal debitBalance) { this.debitBalance = debitBalance; }

    public BigDecimal getCreditBalance() { return creditBalance; }
    public void setCreditBalance(BigDecimal creditBalance) { this.creditBalance = creditBalance; }
}