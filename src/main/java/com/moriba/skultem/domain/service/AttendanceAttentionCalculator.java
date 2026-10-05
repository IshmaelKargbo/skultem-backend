package com.moriba.skultem.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Decides how worried to be about one student's attendance. A flat "rate over the last 30 days"
// breaks down at term boundaries (a window spanning a holiday holds only a few recorded days, so one
// absence swings the rate), flags on a couple of days of data, and can't tell a child who is
// recovering from one who is sliding. This instead looks at recorded school days only:
//
//   * recent window  - the last windowDays recorded days (20 by default, about four weeks). This is what
//                      decides whether a student is flagged, so a child who improves drops out
//                      as soon as the recent window is back above the school's threshold.
//   * previous window - the windowDays before that, only to say improving / declining.
//   * term to date   - tells a chronic problem apart from a bad fortnight.
//   * absence streak - consecutive absences up to the latest recorded day.
//
// A rate needs the school's minimum of recorded days behind it, otherwise it is null ("not enough data")
// rather than a misleading 0% or 50%.
public final class AttendanceAttentionCalculator {

    // Defaults - each school can tune the window / minimum / streak (see School#updateAttendanceRules).
    public static final int DEFAULT_WINDOW_DAYS = 20;
    public static final int DEFAULT_MIN_DAYS = 5;
    public static final int DEFAULT_STREAK_DAYS = 3;
    // Percentage points the recent rate must move against the previous window to count as a trend.
    public static final double TREND_DELTA = 5.0;

    // Declared least to most serious - ordinal comparison is used to pick the worst of two.
    public enum Level {
        NONE, WATCH, NEEDS_ATTENTION, CRITICAL;

        public boolean atLeast(Level other) {
            return compareTo(other) >= 0;
        }
    }

    public enum Trend {
        IMPROVING, STEADY, DECLINING
    }

    // One recorded (non-holiday) school day; attended = present or late.
    public record Day(LocalDate date, boolean attended) {
    }

    // The knobs, all school-configurable. threshold: the attendance % below which a student is a
    // concern. windowDays: recorded school days in the recent (and the previous) window. minDays:
    // recorded days needed before a rate is trusted. streakDays: consecutive absences that are
    // critical on their own.
    public record Rules(double threshold, int windowDays, int minDays, int streakDays) {
        public static Rules defaults(double threshold) {
            return new Rules(threshold, DEFAULT_WINDOW_DAYS, DEFAULT_MIN_DAYS, DEFAULT_STREAK_DAYS);
        }
    }

    public record Result(Double recentRate, Double termRate, Double previousRate, Trend trend, int absenceStreak,
            int recordedDays, Level level) {
    }

    private AttendanceAttentionCalculator() {
    }

    // Groups raw query rows of [enrollmentId, date, attended(0/1)] by enrollment.
    public static Map<String, List<Day>> groupByEnrollment(List<Object[]> rows) {
        Map<String, List<Day>> byEnrollment = new HashMap<>();
        for (Object[] row : rows) {
            byEnrollment.computeIfAbsent((String) row[0], k -> new ArrayList<>())
                    .add(new Day((LocalDate) row[1], ((Number) row[2]).intValue() > 0));
        }
        return byEnrollment;
    }

    public static Result evaluate(List<Day> days, double threshold, LocalDate termStart) {
        return evaluate(days, Rules.defaults(threshold), termStart);
    }

    // days: any order, recorded (non-holiday) days only. termStart: where "term to date" begins, or
    // null to treat everything passed in as the term.
    public static Result evaluate(List<Day> days, Rules rules, LocalDate termStart) {
        if (days == null || days.isEmpty()) {
            return new Result(null, null, null, null, 0, 0, Level.NONE);
        }

        List<Day> newestFirst = new ArrayList<>(days);
        newestFirst.sort(Comparator.comparing(Day::date).reversed());

        List<Day> recent = newestFirst.subList(0, Math.min(rules.windowDays(), newestFirst.size()));
        List<Day> previous = newestFirst.size() > rules.windowDays()
                ? newestFirst.subList(rules.windowDays(), Math.min(rules.windowDays() * 2, newestFirst.size()))
                : List.of();
        List<Day> term = termStart == null ? newestFirst
                : newestFirst.stream().filter(d -> !d.date().isBefore(termStart)).toList();

        Double recentRate = rateOf(recent, rules.minDays());
        Double previousRate = rateOf(previous, rules.minDays());
        Double termRate = rateOf(term, rules.minDays());

        Trend trend = null;
        if (recentRate != null && previousRate != null) {
            double delta = recentRate - previousRate;
            trend = delta >= TREND_DELTA ? Trend.IMPROVING : delta <= -TREND_DELTA ? Trend.DECLINING : Trend.STEADY;
        }

        int streak = 0;
        for (Day d : newestFirst) {
            if (d.attended()) {
                break;
            }
            streak++;
        }

        return new Result(recentRate, termRate, previousRate, trend, streak, recent.size(),
                levelOf(recentRate, termRate, trend, streak, rules));
    }

    private static Level levelOf(Double recentRate, Double termRate, Trend trend, int streak, Rules rules) {
        double threshold = rules.threshold();
        if (streak >= rules.streakDays()) {
            return Level.CRITICAL;
        }
        if (recentRate == null || recentRate >= threshold) {
            return Level.NONE;
        }
        if (trend == Trend.DECLINING) {
            return Level.CRITICAL;
        }
        boolean termBelow = termRate != null && termRate < threshold;
        if (termBelow && trend != Trend.IMPROVING) {
            return Level.NEEDS_ATTENTION;
        }
        // Recent dip only - the term as a whole is fine, or they are already improving.
        return Level.WATCH;
    }

    private static Double rateOf(List<Day> days, int minDays) {
        if (days.size() < minDays) {
            return null;
        }
        long attended = days.stream().filter(Day::attended).count();
        return AttendanceRateCalculator.rate(attended, days.size());
    }
}
