package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExpenseCategoryEntity;

public interface ExpenseCategoryJpaRepository extends JpaRepository<ExpenseCategoryEntity, String> {
    Page<ExpenseCategoryEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<ExpenseCategoryEntity> findByIdAndSchoolId(String id, String schoolId);
}
