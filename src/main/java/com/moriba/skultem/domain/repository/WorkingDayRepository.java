package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.model.WorkingDay;

import java.util.List;
import java.util.Optional;

public interface WorkingDayRepository {
    void save(WorkingDay domain);

    List<WorkingDay> findAllBySchoolId(String school);

    Optional<WorkingDay> findByDayAndSchoolId(WorkingDay.Day day, String school);

    boolean existsByDayAndSchoolId(WorkingDay.Day day, String schoolId);
}
