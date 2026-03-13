package com.moriba.skultem.domain.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AuditLog;

public interface AuditLogRepository {
    void save(AuditLog domain);

    Page<AuditLog> findAllBySchoolIdAndAcademicYear(String schoolId, String academicYearId,
            Pageable pageable);
}
