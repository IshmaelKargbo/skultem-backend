package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.NationalAcademicYearEntity;

public interface NationalAcademicYearJpaRepository extends JpaRepository<NationalAcademicYearEntity, String> {
    Optional<NationalAcademicYearEntity> findFirstByCurrentTrue();

    Optional<NationalAcademicYearEntity> findByNameIgnoreCase(String name);
}
