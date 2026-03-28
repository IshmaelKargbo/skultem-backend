package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateStudentLedgerUsercase {

    private final StudentLedgerEntryRepository repo;
    private final CreateTransactionUsercase createTransactionUsercase;

    @Transactional
    public StudentLedgerEntry createEntry(
            String schoolId,
            String academicYearId,
            String studentId,
            String termId,
            StudentLedgerEntry.TransactionType transactionType,
            StudentLedgerEntry.Direction direction,
            BigDecimal amount,
            String referenceId,
            String description,
            Instant paidAt) {

        StudentLedgerEntry lastEntry = repo.findTopBySchoolIdOrderByPaidAtDesc(schoolId)
                .orElse(null);

        BigDecimal lastBalance = lastEntry != null ? lastEntry.getBalance() : BigDecimal.ZERO;

        BigDecimal newBalance = lastBalance;
        if (direction == StudentLedgerEntry.Direction.DEBIT) {
            newBalance = newBalance.add(amount != null ? amount : BigDecimal.ZERO);
        } else {
            newBalance = newBalance.subtract(amount != null ? amount : BigDecimal.ZERO);
        }

        StudentLedgerEntry entry = StudentLedgerEntry.create(
                java.util.UUID.randomUUID().toString(),
                schoolId,
                academicYearId,
                studentId,
                termId,
                transactionType,
                direction,
                amount,
                referenceId,
                description,
                paidAt,
                newBalance);

        repo.save(entry);

        createTransaction(schoolId, direction, transactionType, amount, referenceId);

        return entry;
    }

    private void createTransaction(String schoolId, StudentLedgerEntry.Direction direction,
            StudentLedgerEntry.TransactionType transactionType, BigDecimal amount, String referenceId) {

        var parseDirection = Transaction.Direction.valueOf(direction.name());
        var parseType = Transaction.TransactionType.valueOf(direction.name());

        createTransactionUsercase.createEntry(schoolId, parseType, parseDirection, amount, referenceId,
                ReferenceType.STUDENT);
    }
}