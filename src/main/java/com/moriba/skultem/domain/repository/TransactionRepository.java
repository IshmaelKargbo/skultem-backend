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
}
