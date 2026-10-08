package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.vo.Level;

public interface StudentRepository {
    void save(Student domain);

    Optional<Student> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByHouseId(String houseId);

    boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId);

    // Same name (case-insensitive) and date of birth - how a bulk import spots a student that's
    // already in the school, e.g. the same file uploaded twice.
    boolean existsByNameAndDateOfBirth(String givenNames, String familyName, LocalDate dateOfBirth,
            String schoolId);

    boolean existsByAdmissionNumberAndSchoolIdAndIdNot(String admissionNumber, String schoolId, String studentId);

    Page<Student> findBySchoolId(String schoolId, Pageable pageable);

    Page<Student> findByParentAndSchoolId(String parentId, String schoolId, Pageable pageable);

    // levels: only students whose class that year is at one of these levels (see SectionScope).
    Page<Student> search(String value, String schoolId, String academicYearId, Status status, String classId, String gender,
            Collection<Level> levels, Pageable pageable);

    long countAll();
}
