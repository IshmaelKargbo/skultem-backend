package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.WorkingDay;

import java.util.List;
import java.util.Optional;

public interface WorkingDayRepository {
    void save(WorkingDay domain);

    // Scoped by timingId rather than schoolId - a school can have several Timing templates, each
    // with its own set of working days (see WorkingDay.timing / TimingLevel).
    List<WorkingDay> findAllByTimingId(String timingId);

    Optional<WorkingDay> findByDayAndTimingId(WorkingDay.Day day, String timingId);

    boolean existsByDayAndTimingId(WorkingDay.Day day, String timingId);

    void deleteAllByTimingId(String timingId);
}
