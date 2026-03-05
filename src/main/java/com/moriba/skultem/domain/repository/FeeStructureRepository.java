package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.FeeStructure;

public interface FeeStructureRepository {
    void save(FeeStructure domain);

    Optional<FeeStructure> findByIdAndSchoolId(String id, String schoolId);

    List<FeeStructure> findApplicableFees(String schoolId, String academicYearId, String classId);

    boolean existsBySchoolAndAcademicYearAndTermAndClassAndCategory(String schoolId, String academicYearId,
            String termId, String classId, String categorId);

    Page<FeeStructure> findBySchoolAndClass(String schoolId, String classId, Pageable pageable);

    Page<FeeStructure> findAllBySchool(String schoolId, Pageable pageable);

    Page<FeeStructure> findBySchoolAndAcademic(String schoolId, String academicYearId, Pageable pageable);

    Page<FeeStructure> findBySchoolAndAcademicAndClass(String schoolId, String academicYearId, String classId,
            Pageable pageable);
}
