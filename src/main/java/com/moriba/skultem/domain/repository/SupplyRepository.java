package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Supply;

public interface SupplyRepository {
    void save(Supply domain);

    boolean existsByStudentIdAndMaterialIdAndSchoolId(String studentId, String materialId, String schoolId);

    Optional<Supply> findByIdAndSchool(String id, String schoolId);

    Page<Supply> findBySchool(String schoolId, Pageable pageable);

    Page<Supply> findByStudentAndSchool(String studentId, String schoolId, Pageable pageable);
}
