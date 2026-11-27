package com.moriba.skultem.application.services;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.usecase.ResolveTermSchoolDaysUseCase;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

import lombok.RequiredArgsConstructor;

// A child's attendance today, this week and last week, day by day - for a parent asking Skully "was she at school
// today?" or "how was his attendance this week?". Only school days count (weekdays in a term, not a holiday or a
// no-school day on the calendar), the same days every attendance figure uses; on any other day Skully can say why
// there was no school.
@Service
@RequiredArgsConstructor
public class SkullyRecentAttendance {

    private final JdbcTemplate jdbc;
    private final ResolveTermSchoolDaysUseCase schoolDays;
    private final AcademicYearRepository academicYearRepo;
    private final HolidayRepository holidayRepo;
    private final CalendarEventRepository calendarRepo;

    // status: PRESENT, LATE, ABSENT, EXCUSED (absent with a reason the school accepted) or NOT_MARKED (the register
    // wasn't taken that day - not an absence).
    public record Day(LocalDate date, String day, String status) {
    }

    // Monday to Friday (this week: up to today). schoolDays: the school days in it so far.
    public record Week(LocalDate from, LocalDate to, int schoolDays, int present, int late, int absent, int excused,
            int notMarked, List<Day> days) {
    }

    // todayStatus: the child's mark today, or null when today isn't a school day - noSchoolReason then says why
    // ("weekend", the holiday or no-school day's name, or "not during a term").
    public record Recent(LocalDate today, String todayName, boolean schoolDayToday, String noSchoolReason,
            String todayStatus, Week thisWeek, Week lastWeek) {
    }

    public Recent of(String schoolId, String studentId) {
        LocalDate today = LocalDate.now(SchoolTimeZone.ZONE);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate lastMonday = monday.minusWeeks(1);

        var open = new java.util.HashSet<>(schoolDays.inActiveYear(schoolId, lastMonday, today));
        Map<LocalDate, String> marks = marks(schoolId, studentId, lastMonday, today);

        boolean schoolDay = open.contains(today);
        return new Recent(today, dayName(today), schoolDay, schoolDay ? null : whyNoSchool(schoolId, today),
                schoolDay ? marks.getOrDefault(today, "NOT_MARKED") : null,
                week(monday, today, open, marks), week(lastMonday, lastMonday.plusDays(4), open, marks));
    }

    static Week week(LocalDate from, LocalDate to, java.util.Set<LocalDate> open, Map<LocalDate, String> marks) {
        List<Day> days = new ArrayList<>();
        int present = 0, late = 0, absent = 0, excused = 0, notMarked = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!open.contains(d)) continue;
            String status = marks.getOrDefault(d, "NOT_MARKED");
            switch (status) {
                case "PRESENT" -> present++;
                case "LATE" -> late++;
                case "ABSENT" -> absent++;
                case "EXCUSED" -> excused++;
                default -> notMarked++;
            }
            days.add(new Day(d, dayName(d), status));
        }
        return new Week(from, to, days.size(), present, late, absent, excused, notMarked, days);
    }

    // The child's mark on each day (any of their enrollments) - late wins over present, as on the Daily Register.
    private Map<LocalDate, String> marks(String schoolId, String studentId, LocalDate from, LocalDate to) {
        Map<LocalDate, String> out = new HashMap<>();
        for (var r : jdbc.queryForList("""
                SELECT a.date, a.present, a.late, a.excused
                FROM attendance a JOIN enrollments e ON e.id = a.enrollment_id
                WHERE a.school_id = ? AND e.student_id = ? AND a.date BETWEEN ? AND ? AND NOT a.holiday
                """, schoolId, studentId, Date.valueOf(from), Date.valueOf(to))) {
            LocalDate date = ((Date) r.get("date")).toLocalDate();
            boolean late = Boolean.TRUE.equals(r.get("late")), present = Boolean.TRUE.equals(r.get("present"));
            out.put(date, late ? "LATE" : present ? "PRESENT" : Boolean.TRUE.equals(r.get("excused")) ? "EXCUSED" : "ABSENT");
        }
        return out;
    }

    private String whyNoSchool(String schoolId, LocalDate day) {
        if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) return "weekend";
        for (var e : calendarRepo.findNoSchoolDays(schoolId)) {
            if (e.getStartDate() == null) continue;
            LocalDate start = e.getStartDate().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate end = e.getEndDate() == null ? start : e.getEndDate().atZone(ZoneOffset.UTC).toLocalDate();
            if (!day.isBefore(start) && !day.isAfter(end)) return e.getTitle();
        }
        var year = academicYearRepo.findActiveBySchool(schoolId);
        if (year.isPresent()) {
            for (var h : holidayRepo.findAllBySchoolIdAndAcademicYear(schoolId, year.get().getId(), Pageable.unpaged())) {
                if (day.equals(h.getDate())) return h.getName();
            }
        }
        return "not during a term";
    }

    private static String dayName(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
