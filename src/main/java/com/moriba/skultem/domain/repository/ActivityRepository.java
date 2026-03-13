package com.moriba.skultem.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Activity;

public interface ActivityRepository {
    void save(Activity domain);

    Page<Activity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
