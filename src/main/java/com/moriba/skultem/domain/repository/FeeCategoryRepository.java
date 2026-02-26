package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.FeeCategory;

public interface FeeCategoryRepository {
    void save(FeeCategory domain);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<FeeCategory> findByIdAndSchool(String id, String schoolId);

    Page<FeeCategory> findBySchool(String schoolId, Pageable pageable);
}
