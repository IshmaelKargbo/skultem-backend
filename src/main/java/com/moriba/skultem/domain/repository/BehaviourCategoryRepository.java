package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.BehaviourCategory;

public interface BehaviourCategoryRepository {
    void save(BehaviourCategory domain);

    boolean existsByNameAndSchool(String name, String schoolId);

    Page<BehaviourCategory> findAllSchoolId(String schoolId, Pageable pageable);

    Optional<BehaviourCategory> findByIdAndSchoolId(String id, String schoolId);
}
