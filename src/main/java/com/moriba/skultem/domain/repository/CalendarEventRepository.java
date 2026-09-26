package com.moriba.skultem.domain.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.CalendarEvent;

public interface CalendarEventRepository {
    void save(CalendarEvent domain);

    void delete(CalendarEvent domain);

    Optional<CalendarEvent> findByIdAndSchool(String id, String schoolId);

    Page<CalendarEvent> findAllBySchoolId(String schoolId, Pageable pageable);

    // Whole-school rows plus those for one of these sections; sectionIds null = no limit.
    Page<CalendarEvent> findVisibleBySchoolId(String schoolId, Collection<String> sectionIds, Pageable pageable);
}
