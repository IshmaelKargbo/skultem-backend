package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.PayrollRunEntity;

public interface PayrollRunJpaRepository extends JpaRepository<PayrollRunEntity, String> {
    Optional<PayrollRunEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<PayrollRunEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<PayrollRunEntity> findFirstBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
