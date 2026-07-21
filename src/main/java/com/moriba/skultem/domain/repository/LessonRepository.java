package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Week;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LessonRepository {
    void save(Week domain);

    List<Week> findAllByWeek(String week);

    Optional<Week> findById(String id);

    boolean existsByWeekIdAndTitleAndSchoolId(String week, String title, String school);

    Page<Week> findByWeekSchemeSessionAcademicYear(String year, Pageable page);
}
