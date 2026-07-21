package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.SaveReportEntity;

public interface SaveReportJpaRepository extends JpaRepository<SaveReportEntity, String> {
    Page<SaveReportEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);
}
