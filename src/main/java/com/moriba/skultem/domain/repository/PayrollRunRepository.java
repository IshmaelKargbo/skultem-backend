package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.PayrollRun;

public interface PayrollRunRepository {
    void save(PayrollRun domain);

    void deleteById(String id);

    Optional<PayrollRun> findByIdAndSchoolId(String id, String schoolId);

    Page<PayrollRun> findAllBySchoolId(String schoolId, Pageable pageable);

    Page<PayrollRun> search(String schoolId, String query, PayrollRun.Status status, Pageable pageable);

    Optional<PayrollRun> findLatestBySchoolId(String schoolId);

    // A run for this period (case/space-insensitive) that has already gone past draft.
    boolean existsFinalizedForPeriod(String schoolId, String period);
}
