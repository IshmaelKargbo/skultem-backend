package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.TimetableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableJpaRepository extends JpaRepository<TimetableEntity, String> {
    boolean existsByPeriodIdAndDayIdAndSchoolId(String period, String day, String schoolId);

    Optional<TimetableEntity> findByPeriodIdAndDayIdAndSchoolId(String period, String day, String schoolId);

    List<TimetableEntity> findByPeriodId(String periodId);
}
