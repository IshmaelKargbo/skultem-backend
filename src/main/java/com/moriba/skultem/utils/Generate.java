package com.moriba.skultem.utils;

import java.math.BigDecimal;
import java.text.Normalizer;

import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;

public class Generate {
    public static String generateSubdomain(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        String cleaned = normalized.replaceAll("[^a-zA-Z0-9]", "-");

        cleaned = cleaned.replaceAll("-+", "-");

        cleaned = cleaned.replaceAll("^-|-$", "");

        return cleaned.toLowerCase();
    }

    public static String generateLedgerDescription(
            TransactionType transactionType,
            String termName,
            String feeOrCategory,
            String studentGivenName,
            String studentFamilyName,
            String admissionNumber,
            BigDecimal amount) {
        StringBuilder desc = new StringBuilder();
        String studentInfo = "";

        if (studentGivenName != null || studentFamilyName != null || admissionNumber != null) {
            studentInfo = " - " +
                    (studentGivenName != null ? studentGivenName + " " : "") +
                    (studentFamilyName != null ? studentFamilyName : "") +
                    (admissionNumber != null ? " (" + admissionNumber + ")" : "");
        }

        switch (transactionType) {
            case FEE_ASSINMENT -> desc.append(feeOrCategory != null ? feeOrCategory : "Fee")
                    .append(" for ")
                    .append(termName)
                    .append(studentInfo);

            case PAYMENT -> desc.append("Payment of ")
                    .append(amount)
                    .append(" for ")
                    .append(termName)
                    .append(studentInfo);

            case DISCOUNT -> desc.append(feeOrCategory != null ? feeOrCategory : "Discount")
                    .append(" applied for ")
                    .append(termName)
                    .append(studentInfo);

            case ADJUSTMENT -> desc.append("Adjustment for ")
                    .append(termName)
                    .append(studentInfo);

            default -> desc.append(transactionType.name()).append(" for ").append(termName).append(studentInfo);
        }

        return desc.toString();
    }
}
