package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Week;

import java.util.List;
import java.util.Optional;

public interface WeekRepository {
    void save(Week domain);

    List<Week> findAllByScheme(String scheme);

    Optional<Week> findById(String id);

    boolean existsByWeekAndSchemeAndSchoolId(int week, String scheme, String school);
}
