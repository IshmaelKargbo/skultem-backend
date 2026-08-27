package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.WeekEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeekJpaRepository extends JpaRepository<WeekEntity, String> {
    boolean existsByWeekAndScheme_IdAndSchoolId(int week, String scheme, String school);

    Optional<WeekEntity> findById(String id);

    List<WeekEntity> findBySchemeId(String scheme);

    List<WeekEntity> findBySchemeIdIn(List<String> schemeIds);

    Page<WeekEntity> findBySchemeSessionAcademicYearId(String year, Pageable page);
}
