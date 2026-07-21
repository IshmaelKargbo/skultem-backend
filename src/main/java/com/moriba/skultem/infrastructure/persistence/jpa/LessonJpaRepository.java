package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.LessonEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonJpaRepository extends JpaRepository<LessonEntity, String> {
    boolean existsByWeekIdAndTitleAndSchoolId(String week, String title, String school);

    Optional<LessonEntity> findById(String id);

    List<LessonEntity> findByWeekId(String week);

    Page<LessonEntity> findByWeekSchemeSessionAcademicYearId(String year, Pageable page);
}
