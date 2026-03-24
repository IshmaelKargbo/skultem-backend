package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ExpenseEntity;

@Repository
public interface ExpenseJpaRepository extends JpaRepository<ExpenseEntity, String> {
    Page<ExpenseEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<ExpenseEntity> findByIdAndSchoolId(String id, String schoolId);
}
