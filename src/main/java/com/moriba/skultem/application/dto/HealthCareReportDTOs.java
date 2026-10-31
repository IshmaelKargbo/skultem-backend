package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** The Health Care dashboard, checks shown before acting, and reports (docs/HEALTH_CARE.md §5.2, §8, §10). */
public final class HealthCareReportDTOs {

    private HealthCareReportDTOs() {
    }

    /** The nurse's home: who's in the sick bay now, what needs following up, and today in numbers. */
    public record Overview(List<HealthCareVisitDTO> inSickBay, List<HealthCareVisitDTO> followUp, Today today,
            Integer longStayMinutes) {
    }

    public record Today(long visits, long open, long returnedToClass, long sentHome, long referred, long turnedAway) {
    }

    /**
     * Shown when the nurse picks a student and complaints, before starting a visit (V-3, §6.2).
     * {@code openVisitId}: the student already has an open visit (V-1) - close or follow that up first.
     * {@code continuable}: today's closed visits a new visit can continue (V-9).
     */
    public record Check(String studentId, String studentName, String studentClass, long visitsThisWeek,
            long visitsThisTerm, Instant lastVisitAt, String lastVisitComplaints, String openVisitId, Limit limit,
            List<Continuable> continuable) {
    }

    /** A limit rule that was reached, or {@code result = ALLOW} with nothing else set. */
    public record Limit(String result, String ruleName, Integer maxVisits, String period, long count, boolean urgent,
            boolean refused, boolean needsReason, String message) {
    }

    public record Continuable(String id, Instant arrivedAt, String complaints, String outcome) {
    }

    /** Starting a visit: either the visit, or a turned-away entry when a Refuse rule stopped it (L-5). */
    public record StartResult(HealthCareVisitDTO visit, boolean turnedAway, String message) {
    }

    /** The same medicine was given to this student within the school's window (M-2). Null when it wasn't. */
    public record MedicationCheck(String name, Instant lastGivenAt, long minutesAgo, int windowHours, String action,
            String message) {
    }

    public record Count(String key, String label, long count) {
    }

    /** Visits by period (§10): voided ones are left out; open ones are counted as still open. */
    public record Summary(LocalDate from, LocalDate to, long visits, long students, long urgent, long stillOpen,
            long overLimit, long turnedAway, Long averageMinutes, List<Count> byComplaint, List<Count> byOutcome,
            List<ClassRow> classes, List<DayRow> days) {
    }

    /** One class's visits in the range, by outcome. {@code students}: different students seen. */
    public record ClassRow(String className, long visits, long students, long returnedToClass, long sentHome,
            long referred, long stillOpen) {
    }

    /** Visits on one day of the range (days without visits included, so the trend has no gaps). */
    public record DayRow(LocalDate date, long visits, long sentHome, long referred) {
    }

    public record FrequentVisitor(String studentId, String studentName, String studentClass, long visits,
            List<Instant> dates, List<String> complaints) {
    }

    public record TurnedAway(String id, String studentId, String studentName, String studentClass, Instant at,
            String complaints, String ruleName, int maxVisits, String period, int visitCount, String recordedByName) {
    }

    /** The academic years (with their terms) a trends report can cover, and which are current. */
    public record Periods(String activeYearId, String activeTermId, List<YearOption> years) {
    }

    public record YearOption(String id, String name, LocalDate start, LocalDate end, List<TermOption> terms) {
    }

    public record TermOption(String id, String name, LocalDate start, LocalDate end) {
    }

    /** One column of a trends report: a week of a month, a month of a term, or a term of a year. */
    public record Bucket(String key, String label, LocalDate from, LocalDate to) {
    }

    /** One row of a trends report: a count per bucket (same order as the buckets) and the total. */
    public record TrendRow(String key, String label, boolean urgent, List<Long> counts, long total) {
    }

    public record MedicineRow(String name, long times, long students) {
    }

    /**
     * Complaints and other figures over a month (by week), a term (by month) or an academic year (by term), for
     * spotting patterns: what students come in with, when, from which classes, and what is given to them.
     * Voided visits are left out. Visits outside every bucket (e.g. holidays in a year) get an "Outside terms" column.
     */
    public record Trends(String scope, String title, LocalDate from, LocalDate to, List<Bucket> buckets,
            List<TrendRow> overview, List<TrendRow> complaints, List<TrendRow> outcomes, List<TrendRow> classes,
            List<MedicineRow> medicines, List<Count> byWeekday, List<Count> byHour) {
    }

    /** Over-limit visits (by rule, with reasons) and turned-away entries in a range (§10, L-8). */
    public record Limits(LocalDate from, LocalDate to, List<HealthCareVisitDTO> overLimit, List<TurnedAway> turnedAway) {
    }

    /** One student's record: their visits (voided ones flagged) and any visits a limit turned away. */
    public record StudentHistory(String studentId, String studentName, List<HealthCareVisitDTO> visits,
            List<TurnedAway> turnedAway) {
    }
}
