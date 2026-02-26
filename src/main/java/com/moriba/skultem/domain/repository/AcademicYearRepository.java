package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AcademicYear;

public interface AcademicYearRepository {
    void save(AcademicYear domain);

    Optional<AcademicYear> findById(String id);

    Optional<AcademicYear> findByIdAndSchoolId(String id, String school);

    boolean existsByNameAndSchool(String school, String name);

    Optional<AcademicYear> findActiveBySchool(String school);

    void deactivateAllBySchool(String school);
   
    Page<AcademicYear> findAllBySchool(String school, Pageable pageable);

    void delete(AcademicYear domain);
}
