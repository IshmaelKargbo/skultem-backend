package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Timetable;

import java.util.List;
import java.util.Optional;

public interface TimetableRepository {
    void save(Timetable domain);

    Optional<Timetable> findByPeriodIdAndDayIdAndSchoolId(String period, String day, String schoolId);

    List<Timetable> findAllByPeriodId(String period);

    boolean existsByPeriodAndDayAndSchoolId(String period, String day, String schoolId);

    void delete(Timetable domain);
}
