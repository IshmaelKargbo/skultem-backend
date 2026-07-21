package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AuditLogEntity;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {

    Page<AuditLogEntity> findAllBySchoolIdAndAcademicYear_Id(String schoolId, String academicYearId,
            Pageable pageable);
}
