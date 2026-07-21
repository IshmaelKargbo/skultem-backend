package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;

public interface StreamJpaRepository extends JpaRepository<StreamEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Page<StreamEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<StreamEntity> findByIdAndSchoolId(String id, String schoolId);
}
