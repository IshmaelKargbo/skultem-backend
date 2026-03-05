package com.moriba.skultem.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.BehaviourCategory;

public interface BehaviourCategoryRepository {
    void save(BehaviourCategory domain);

    Page<BehaviourCategory> findAllSchoolId(String schoolId, Pageable pageable);
}
