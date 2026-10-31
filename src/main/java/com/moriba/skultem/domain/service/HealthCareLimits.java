package com.moriba.skultem.domain.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitAction;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;
import com.moriba.skultem.domain.model.HealthCareSetting.Period;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.Level;

/**
 * Whether a student may start another sick bay visit under the school's limit rules (docs/HEALTH_CARE.md §6.2).
 * A pure calculation: the caller passes the arrival times of the student's counted visits (not voided, not
 * continuations - L-1, L-3), so it's easy to test and has no database behind it.
 * <p>
 * Every enabled rule that applies to the student's level is checked; a rule is "reached" when the student already
 * has {@code maxVisits} counted visits in the rule's period. The strictest reached rule wins (S-5). Urgent
 * complaints are never refused and never wait for a reason (L-4) - the visit is only flagged.
 */
public final class HealthCareLimits {

    private HealthCareLimits() {
    }

    /** The first and last day (inclusive) of the current term, or null when today isn't in a term. */
    public record TermDates(LocalDate start, LocalDate end) {
    }

    public enum Result {
        /** No rule reached. */
        ALLOW,
        /** A rule was reached; the visit goes ahead with a message. */
        WARN,
        /** A rule was reached; the visit needs a reason. */
        REASON,
        /** A rule was reached; the visit is refused (unless urgent). */
        REFUSE
    }

    /**
     * @param result  the strictest reached rule's action, or ALLOW
     * @param rule    that rule, or null
     * @param count   the student's counted visits in that rule's period, before this one
     * @param urgent  the new visit has an urgent complaint
     * @param message what the nurse sees, or null when nothing was reached
     */
    public record Check(Result result, LimitRule rule, long count, boolean urgent, String message) {

        public boolean reached() {
            return rule != null;
        }

        /** Refused: a Refuse rule was reached and nothing is urgent. */
        public boolean refused() {
            return result == Result.REFUSE && !urgent;
        }

        /** Needs a reason: an "Ask for a reason" rule was reached and nothing is urgent. */
        public boolean needsReason() {
            return result == Result.REASON && !urgent;
        }
    }

    public static Check check(HealthCareSetting setting, Level level, boolean urgent, List<Instant> countedArrivals,
            LocalDate today, TermDates term) {
        LimitRule worst = null;
        long worstCount = 0;
        for (LimitRule rule : setting.getLimitRules()) {
            if (!rule.appliesTo(level)) {
                continue;
            }
            var window = window(rule.period(), today, term);
            if (window == null) {
                continue;
            }
            long count = countBetween(countedArrivals, window[0], window[1]);
            if (count < rule.maxVisits()) {
                continue;
            }
            if (worst == null || stricter(rule.action(), worst.action())) {
                worst = rule;
                worstCount = count;
            }
        }
        if (worst == null) {
            return new Check(Result.ALLOW, null, 0, urgent, null);
        }
        return new Check(Result.valueOf(worst.action().name()), worst, worstCount, urgent,
                message(worst, worstCount, urgent));
    }

    private static boolean stricter(LimitAction a, LimitAction b) {
        return a.ordinal() > b.ordinal();
    }

    /** First and last day (inclusive) of the period containing {@code today}; null for a term when out of term. */
    public static LocalDate[] window(Period period, LocalDate today, TermDates term) {
        return switch (period) {
            case DAY -> new LocalDate[] { today, today };
            case WEEK -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                yield new LocalDate[] { monday, monday.plusDays(6) };
            }
            case MONTH -> new LocalDate[] { today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()) };
            case TERM -> term == null || today.isBefore(term.start()) || today.isAfter(term.end()) ? null
                    : new LocalDate[] { term.start(), term.end() };
        };
    }

    public static long countBetween(List<Instant> arrivals, LocalDate first, LocalDate last) {
        return arrivals.stream()
                .map(a -> LocalDate.ofInstant(a, SchoolTimeZone.ZONE))
                .filter(d -> !d.isBefore(first) && !d.isAfter(last))
                .count();
    }

    public static String periodPhrase(Period period) {
        return switch (period) {
            case DAY -> "today";
            case WEEK -> "this week";
            case MONTH -> "this month";
            case TERM -> "this term";
        };
    }

    private static String message(LimitRule rule, long count, boolean urgent) {
        String visits = count == 1 ? "1 visit" : count + " visits";
        String limit = "\"" + rule.name() + "\" allows " + rule.maxVisits() + " " + periodPhrase(rule.period()) + ".";
        String base = "Already had " + visits + " " + periodPhrase(rule.period()) + ". " + limit;
        if (urgent && rule.action() != LimitAction.WARN) {
            return base + " The complaint is urgent, so the visit goes ahead and is flagged as over the limit.";
        }
        return switch (rule.action()) {
            case WARN -> base;
            case REASON -> base + " Give a reason to start this visit.";
            case REFUSE -> base + " This visit can't be started. If the student needs care, choose an urgent "
                    + "complaint or speak to the principal.";
        };
    }
}
