package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.MaterialEntity;

public interface MaterialJpaRepository extends JpaRepository<MaterialEntity, String> {
    Page<MaterialEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<MaterialEntity> findByIdAndSchoolId(String id, String schoolId);
}
