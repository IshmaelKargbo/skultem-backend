package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Week;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WeekRepository {
    void save(Week domain);

    List<Week> findAllByScheme(String scheme);

    List<Week> findAllBySchemeIds(List<String> schemeIds);

    Optional<Week> findById(String id);

    boolean existsByWeekAndSchemeAndSchoolId(int week, String scheme, String school);

    Page<Week> findBySchemeSessionAcademicYear(String year, Pageable page);
}
