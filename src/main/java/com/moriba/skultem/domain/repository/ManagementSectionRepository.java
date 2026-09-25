package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.ManagementSection;

public interface ManagementSectionRepository {
    void save(ManagementSection domain);

    List<ManagementSection> findBySchoolId(String schoolId);

    void delete(ManagementSection domain);
}
