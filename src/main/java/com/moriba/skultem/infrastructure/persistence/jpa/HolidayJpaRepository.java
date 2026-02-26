package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.HolidayEntity;

@Repository
public interface HolidayJpaRepository extends JpaRepository<HolidayEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<HolidayEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<HolidayEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Page<HolidayEntity> findAllBySchoolIdAndAcademicYear_Id(String schoolId, String academicYearId, Pageable pageable);
}
