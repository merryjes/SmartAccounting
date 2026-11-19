package com.smartaccounting.service;

import com.smartaccounting.model.JournalEntry;
import com.smartaccounting.model.JournalLine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransactionValidator {

    public static List<String> validate(JournalEntry entry) {
        List<String> errors = new ArrayList<>();

        if (entry == null) {
            errors.add("Journal entry cannot be null.");
            return errors;
        }
        if (entry.getEntryDate() == null) {
            errors.add("Transaction date is required.");
        }
        if (entry.getReferenceNo() == null || entry.getReferenceNo().trim().isEmpty()) {
            errors.add("Transaction reference number is required.");
        }
        if (entry.getDescription() == null || entry.getDescription().trim().isEmpty()) {
            errors.add("Transaction description is required.");
        }

        List<JournalLine> lines = entry.getLines();
        if (lines == null || lines.size() < 2) {
            errors.add("At least two account lines are required for a journal entry.");
            return errors;
        }

        for (int i = 0; i < lines.size(); i++) {
            JournalLine line = lines.get(i);
            int lineNum = i + 1;

            if (line.getAccountId() <= 0) {
                errors.add("Line " + lineNum + ": Please select a valid account.");
            }

            BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Line " + lineNum + ": Debit and credit amounts cannot be negative.");
            }
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                errors.add("Line " + lineNum + ": A single line cannot have both a debit and a credit.");
            }
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                errors.add("Line " + lineNum + ": Line must have either a debit or a credit amount greater than zero.");
            }
        }

        BigDecimal totalDebit = entry.getTotalDebit();
        BigDecimal totalCredit = entry.getTotalCredit();

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Total debit must be greater than zero.");
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            errors.add("Unbalanced entry: Total Debit (" + totalDebit +
                       ") does not equal Total Credit (" + totalCredit + ").");
        }

        return errors;
    }
}