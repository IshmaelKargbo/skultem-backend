package com.moriba.skultem.domain.repository;

import java.util.List;

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

    // Whole-school events starting at or after from, soonest first.
    List<CalendarEvent> findUpcomingSchoolWide(String schoolId, java.time.Instant from, int limit);

    // Whole-school rows plus those for one of these sections; sectionIds null = no limit.
    Page<CalendarEvent> findVisibleBySchoolId(String schoolId, Collection<String> sectionIds, Pageable pageable);
}
