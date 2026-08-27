package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
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

    /**
     * Finds the earliest configured academic year for the school that starts after the given date,
     * used to resolve where a promoted class/enrollment should land.
     */
    Optional<AcademicYear> findNextBySchool(String school, LocalDate after);

    /**
     * Finds the latest configured academic year for the school that ended before the given date -
     * the mirror of {@link #findNextBySchool}, used to locate a new class session's predecessor so
     * its class master and teacher/subject assignments can be carried forward into the new year.
     */
    Optional<AcademicYear> findPreviousBySchool(String school, LocalDate before);

    /**
     * Whether some other academic year already points to this one as its next year - checked before
     * a delete so we never leave a {@link AcademicYear#getNextYear()} link dangling.
     */
    boolean existsAsNextYear(String school, String yearId);
}
