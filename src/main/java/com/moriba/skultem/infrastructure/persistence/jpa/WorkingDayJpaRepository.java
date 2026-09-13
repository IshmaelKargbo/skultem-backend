package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.infrastructure.persistence.entity.WorkingDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkingDayJpaRepository extends JpaRepository<WorkingDayEntity, String> {
    boolean existsByDayAndTiming_Id(WorkingDay.Day day, String timingId);

    Optional<WorkingDayEntity> findByDayAndTiming_Id(WorkingDay.Day day, String timingId);

    List<WorkingDayEntity> findAllByTiming_Id(String timingId);

    void deleteAllByTiming_Id(String timingId);
}
