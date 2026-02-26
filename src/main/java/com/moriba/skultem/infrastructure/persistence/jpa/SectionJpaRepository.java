package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.SectionEntity;

@Repository
public interface SectionJpaRepository extends JpaRepository<SectionEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Page<SectionEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<SectionEntity> findByNameAndSchoolId(String name, String schoolId);

    Optional<SectionEntity> findByIdAndSchoolId(String id, String schoolId);
}
