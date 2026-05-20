package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.MaterialCategory;

public interface MaterialCategoryRepository {
    void save(MaterialCategory domain);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<MaterialCategory> findByIdAndSchool(String id, String schoolId);

    Page<MaterialCategory> findBySchool(String schoolId, Pageable pageable);
}
