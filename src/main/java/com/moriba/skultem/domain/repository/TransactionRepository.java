package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.vo.Filter;

public interface TransactionRepository {
        void save(Transaction domain);

        Optional<Transaction> findTopBySchoolId(String schoolId);

        Page<Transaction> findAllByAcademicYearAndSchool(String academicYearId, String schoolId,
                        Pageable pageable);

        Page<Transaction> runReport(String schoolId, List<Filter> filters, Pageable pageable);

        /** The school's transactions for one academic year, narrowed by type, direction, reference type and a date range (each ignored when null). */
        Page<Transaction> searchTransactions(String schoolId, String academicYearId, Transaction.TransactionType type,
                        Transaction.Direction direction, Transaction.ReferenceType referenceType,
                        java.time.Instant from, java.time.Instant toExclusive, Pageable pageable);

        /**
         * Wipes a test school's STUDENT transactions only - see WipeTestSchoolDataUseCase. Expense,
         * payroll and material-sale transactions aren't roster/activity data and are left alone.
         */
        void deleteAllBySchoolIdAndReferenceType(String schoolId, Transaction.ReferenceType referenceType);
}
