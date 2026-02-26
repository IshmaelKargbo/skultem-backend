package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Holiday;

public interface HolidayRepository {
    void save(Holiday domain);

    void delete(Holiday domain);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<Holiday> findByIdAndSchool(String id, String schoolId);

    Page<Holiday> findAllBySchoolId(String schoolId, Pageable pageable);

    Page<Holiday> findAllBySchoolIdAndAcademicYear(String schoolId, String academicYearId, Pageable pageable);
}
