package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.repository.TransactionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTransactionUsercase {

    private final AcademicYearRepository academicYearRepo;
    private final TermRepository termRepo;
    private final TransactionRepository repo;

    @Transactional
    public Transaction createEntry(
            String schoolId,
            Transaction.TransactionType transactionType,
            Transaction.Direction direction,
            BigDecimal amount,
            String referenceId,
            Transaction.ReferenceType referenceType) {

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        var term = termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                .orElseThrow(() -> new NotFoundException("Active term not found"));

        Transaction lastEntry = repo.findTopBySchoolId(schoolId)
                .orElse(null);

        BigDecimal lastBalance = lastEntry != null ? lastEntry.getBalance() : BigDecimal.ZERO;

        BigDecimal newBalance = lastBalance;
        if (direction == Transaction.Direction.DEBIT) {
            newBalance = newBalance.add(amount != null ? amount : BigDecimal.ZERO);
        } else {
            newBalance = newBalance.subtract(amount != null ? amount : BigDecimal.ZERO);
        }

        Transaction entry = Transaction.create(
                java.util.UUID.randomUUID().toString(), schoolId, academicYear.getId(), term.getId(), transactionType,
                direction, amount, newBalance, referenceId, referenceType);

        repo.save(entry);
        return entry;
    }
}