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

    boolean existsBySchoolAndAcademicYearAndTermAndClassAndCategory(String schoolId, String academicYearId,
            String termId, String classId, String categorId);

    boolean existsByCategoryAndSchool(String categoryId, String schoolId);

    boolean existsSystemFeeBySchoolAndAcademicYear(String schoolId, String academicYearId);

    Optional<FeeStructure> findSystemFeeBySchoolAndAcademicYear(String schoolId, String academicYearId);

    Page<FeeStructure> findBySchoolAndClass(String schoolId, String classId, Pageable pageable);

    Page<FeeStructure> findAllBySchool(String schoolId, Pageable pageable);

    Page<FeeStructure> search(String schoolId, String termId, Pageable pageable);

    Page<FeeStructure> findBySchoolAndAcademic(String schoolId, String academicYearId, Pageable pageable);

    Page<FeeStructure> findBySchoolAndAcademicAndClass(String schoolId, String academicYearId, String classId,
            Pageable pageable);
}
