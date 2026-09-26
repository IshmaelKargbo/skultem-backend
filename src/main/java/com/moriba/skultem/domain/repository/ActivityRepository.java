package com.moriba.skultem.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Activity;

public interface ActivityRepository {
    void save(Activity domain);

    Page<Activity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    // What a section-limited admin may see: activity in their own section(s), plus school-wide announcements (notices
    // and broadcasts, which carry no section).
    Page<Activity> findVisibleToSections(String schoolId, java.util.Collection<String> sectionIds, Pageable pageable);
}
