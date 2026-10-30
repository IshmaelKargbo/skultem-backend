package com.moriba.skultem.application.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Holiday;
import com.moriba.skultem.domain.model.LibrarySession.Slot;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.repository.TimetableRepository;

import lombok.RequiredArgsConstructor;

/**
 * Works out the scheduled library sessions from the school's own timetable - there is no separate
 * library timetable (docs/LIBRARY_ATTENDANCE.md, B-1 to B-8).
 * <p>
 * A Library timetable entry (an entry for the school's chosen Library subject, in a teaching period)
 * has a session on a date when the date's weekday is the entry's working day, that day is switched on,
 * the date falls in a term of the active academic year, and it isn't a holiday. Sessions on holidays
 * are not shown at all.
 */
@Component
@RequiredArgsConstructor
public class LibrarySessionSchedule {

    /** Longest date range listed in one go. */
    public static final int MAX_DAYS = 92;

    private final LibrarySettingRepository settingRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TermRepository termRepo;
    private final HolidayRepository holidayRepo;
    private final TimetableRepository timetableRepo;

    /** Everything needed to work out a school's sessions, loaded once. */
    public record Calendar(String subjectId, List<Timetable> entries, List<Term> terms, Set<LocalDate> holidays) {
    }

    /** Empty when the school hasn't chosen a Library subject or has no active academic year. */
    public Optional<Calendar> load(String schoolId) {
        var subjectId = settingRepo.findBySchool(schoolId).map(s -> s.getLibrarySubjectId()).orElse(null);
        if (subjectId == null) {
            return Optional.empty();
        }
        var year = academicYearRepo.findActiveBySchool(schoolId);
        if (year.isEmpty()) {
            return Optional.empty();
        }
        String yearId = year.get().getId();
        var entries = timetableRepo.findAllBySubjectAndAcademicYear(subjectId, yearId, schoolId).stream()
                .filter(t -> t.getPeriod() != null && t.getPeriod().getType() == Period.Type.PERIOD)
                .toList();
        var terms = termRepo.findByAcademicYearIdAndSchool(yearId, schoolId);
        Set<LocalDate> holidays = holidayRepo
                .findAllBySchoolIdAndAcademicYear(schoolId, yearId, Pageable.unpaged())
                .getContent().stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());
        return Optional.of(new Calendar(subjectId, entries, terms, holidays));
    }

    public List<Slot> slotsBetween(Calendar calendar, LocalDate from, LocalDate to) {
        List<Slot> slots = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (Timetable entry : calendar.entries()) {
                slotOn(calendar, entry, date).ifPresent(slots::add);
            }
        }
        slots.sort(Comparator.comparing(Slot::date).thenComparing(Slot::startTime).thenComparing(Slot::className));
        return slots;
    }

    /** The session for one Library entry on one date, or a refusal saying why there isn't one. */
    public Slot slotFor(String schoolId, String timetableEntryId, LocalDate date) {
        var calendar = load(schoolId)
                .orElseThrow(() -> new RuleException("Choose the school's Library subject in Library Settings first"));
        var entry = calendar.entries().stream()
                .filter(t -> t.getId().equals(timetableEntryId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Library period not found on the timetable"));
        return slotOn(calendar, entry, date)
                .orElseThrow(() -> new RuleException("There is no library session for this class on " + date));
    }

    private Optional<Slot> slotOn(Calendar calendar, Timetable entry, LocalDate date) {
        var day = entry.getDay();
        if (day == null || !day.isState() || !day.getDay().name().equals(date.getDayOfWeek().name())) {
            return Optional.empty();
        }
        if (calendar.holidays().contains(date)) {
            return Optional.empty();
        }
        var term = calendar.terms().stream()
                .filter(t -> t.getStartDate() != null && t.getEndDate() != null)
                .filter(t -> !date.isBefore(t.getStartDate()) && !date.isAfter(t.getEndDate()))
                .findFirst();
        if (term.isEmpty()) {
            return Optional.empty();
        }
        var period = entry.getPeriod();
        var session = period.getSession();
        var subject = entry.getTeacherSubject().getSubject();
        return Optional.of(new Slot(entry.getId(), date, session.getId(), session.getName(), subject.getId(),
                subject.getName(), period.getName(), period.getStartTime(), period.getEndTime(),
                entry.getRoom() == null ? null : entry.getRoom().getName(), term.get().getId(),
                term.get().getEndDate()));
    }
}
