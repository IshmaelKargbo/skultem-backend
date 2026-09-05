package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.SalaryStructure;

public interface SalaryStructureRepository {
    void save(SalaryStructure domain);

    Optional<SalaryStructure> findByIdAndSchoolId(String id, String schoolId);

    Optional<SalaryStructure> findByTeacherIdAndSchoolId(String teacherId, String schoolId);

    boolean existsByTeacherIdAndSchoolId(String teacherId, String schoolId);

    Page<SalaryStructure> search(String value, String schoolId, Pageable pageable);

    List<SalaryStructure> findAllBySchoolId(String schoolId);

    Page<SalaryStructure> findAllBySchoolId(String schoolId, Pageable pageable);
}
