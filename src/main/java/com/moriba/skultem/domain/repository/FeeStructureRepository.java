package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.FeeStructure;

public interface FeeStructureRepository {
    void save(FeeStructure domain);

    void deleteById(String id);

    Optional<FeeStructure> findByIdAndSchoolId(String id, String schoolId);

    List<FeeStructure> findApplicableFees(String schoolId, String academicYearId, String classId);

    // See FeeStructureJpaRepository.existsOverlappingFeeStructure - a plain fee overlaps everything
    // for the same class/term/category/year, while newStudentsOnly and oldStudentsOnly never
    // overlap each other since they're a deliberate partition of the same roster.
    boolean existsOverlappingFeeStructure(String schoolId, String academicYearId, String termId, String classId,
            String categoryId, boolean newStudentsOnly, boolean oldStudentsOnly);

    boolean existsByCategoryAndSchool(String categoryId, String schoolId);

    boolean existsSystemFeeBySchoolAndAcademicYear(String schoolId, String academicYearId);

    Optional<FeeStructure> findSystemFeeBySchoolAndAcademicYear(String schoolId, String academicYearId);

    Page<FeeStructure> findBySchoolAndClass(String schoolId, String classId, Pageable pageable);

    Page<FeeStructure> findAllBySchool(String schoolId, Pageable pageable);

    Page<FeeStructure> search(String schoolId, String termId, String classId, Boolean newStudentsOnly,
            Boolean oldStudentsOnly, Pageable pageable);

    Page<FeeStructure> findBySchoolAndAcademic(String schoolId, String academicYearId, Pageable pageable);

    Page<FeeStructure> findBySchoolAndAcademicAndClass(String schoolId, String academicYearId, String classId,
            Pageable pageable);
}
