package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.NationalTermEntity;

public interface NationalTermJpaRepository extends JpaRepository<NationalTermEntity, String> {
    List<NationalTermEntity> findAllByNationalAcademicYearIdOrderByTermNumberAsc(String nationalAcademicYearId);

    void deleteAllByNationalAcademicYearId(String nationalAcademicYearId);
}
