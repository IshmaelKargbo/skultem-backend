package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.FeeCategoryEntity;

@Repository
public interface FeeCategoryJpaRepository extends JpaRepository<FeeCategoryEntity, String> {
    Page<FeeCategoryEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<FeeCategoryEntity> findByIdAndSchoolId(String id, String schoolId);
}
