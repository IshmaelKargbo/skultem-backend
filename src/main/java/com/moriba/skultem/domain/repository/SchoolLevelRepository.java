package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.SchoolLevel;

public interface SchoolLevelRepository {
    void save(SchoolLevel domain);

    List<SchoolLevel> findBySchoolId(String schoolId);

    void delete(SchoolLevel domain);
}
