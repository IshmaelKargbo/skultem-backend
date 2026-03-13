package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ReportConfigEntity;

@Repository
public interface ReportConfigJpaRepository extends JpaRepository<ReportConfigEntity, String> {
    Page<ReportConfigEntity> findAllBySchoolId(String schoolId, Pageable pageable);
}
