package com.moriba.skultem.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitAction;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;
import com.moriba.skultem.domain.model.HealthCareSetting.Period;
import com.moriba.skultem.domain.service.HealthCareLimits.Result;
import com.moriba.skultem.domain.service.HealthCareLimits.TermDates;
import com.moriba.skultem.domain.vo.Level;

// docs/HEALTH_CARE.md §6.2: limit rules, calendar periods, levels, strictest wins, urgent never refused.
class HealthCareLimitsTest {

    // Wednesday. Freetown is UTC, so these instants are local times.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final TermDates TERM = new TermDates(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 12, 18));

    private static HealthCareSetting with(LimitRule... rules) {
        var setting = HealthCareSetting.defaults("school");
        var d = HealthCareSetting.defaultValues();
        setting.update(new HealthCareSetting.Values(List.of(rules), d.medicationAllowed(), d.sameMedicineHours(),
                d.sameMedicineAction(), d.maxMedicationLines(), d.requireBroughtBy(), d.requireTemperature(),
                d.requireTreatment(), d.requireNotesOnReturn(), d.notifyOutcomes(), d.allowManualNotify(),
                d.notifyPrincipalOnReferral(), d.principalAlertVisits(), d.principalAlertPeriod(),
                d.longStayMinutes(), d.complaints()), "p", "Principal", Instant.now());
        return setting;
    }

    private static LimitRule rule(String name, int max, Period period, LimitAction action, Level... levels) {
        return new LimitRule(null, name, List.of(levels), max, period, action, true);
    }

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    @Test
    void noRulesAllowsEverything() {
        var check = HealthCareLimits.check(HealthCareSetting.defaults("school"), Level.JSS, false,
                List.of(at("2026-10-27T09:00:00Z"), at("2026-10-28T08:00:00Z")), TODAY, TERM);
        assertThat(check.result()).isEqualTo(Result.ALLOW);
        assertThat(check.reached()).isFalse();
    }

    @Test
    void oneAWeekRefusesTheSecondVisitInTheSameMondayToSundayWeek() {
        var setting = with(rule("One a week", 1, Period.WEEK, LimitAction.REFUSE));
        var check = HealthCareLimits.check(setting, Level.JSS, false, List.of(at("2026-10-26T10:00:00Z")), TODAY, TERM);
        assertThat(check.refused()).isTrue();
        assertThat(check.count()).isEqualTo(1);
        assertThat(check.message()).contains("this week");
    }

    @Test
    void aVisitLastWeekDoesNotCountThisWeek() {
        var setting = with(rule("One a week", 1, Period.WEEK, LimitAction.REFUSE));
        // Sunday 25th is the previous Monday-Sunday week.
        var check = HealthCareLimits.check(setting, Level.JSS, false, List.of(at("2026-10-25T10:00:00Z")), TODAY, TERM);
        assertThat(check.result()).isEqualTo(Result.ALLOW);
    }

    @Test
    void urgentComplaintsAreNeverRefusedButAreFlagged() {
        var setting = with(rule("One a week", 1, Period.WEEK, LimitAction.REFUSE));
        var check = HealthCareLimits.check(setting, Level.JSS, true, List.of(at("2026-10-26T10:00:00Z")), TODAY, TERM);
        assertThat(check.refused()).isFalse();
        assertThat(check.needsReason()).isFalse();
        assertThat(check.reached()).isTrue();
        assertThat(check.message()).contains("urgent");
    }

    @Test
    void askForAReasonNeedsAReasonUnlessUrgent() {
        var setting = with(rule("Two a day", 2, Period.DAY, LimitAction.REASON));
        var arrivals = List.of(at("2026-10-28T08:00:00Z"), at("2026-10-28T09:00:00Z"));
        assertThat(HealthCareLimits.check(setting, Level.JSS, false, arrivals, TODAY, TERM).needsReason()).isTrue();
        assertThat(HealthCareLimits.check(setting, Level.JSS, true, arrivals, TODAY, TERM).needsReason()).isFalse();
    }

    @Test
    void aRuleForOneLevelDoesNotAffectOthers() {
        var setting = with(rule("Nursery: one a day", 1, Period.DAY, LimitAction.REFUSE, Level.NURSERY));
        var arrivals = List.of(at("2026-10-28T08:00:00Z"));
        assertThat(HealthCareLimits.check(setting, Level.NURSERY, false, arrivals, TODAY, TERM).refused()).isTrue();
        assertThat(HealthCareLimits.check(setting, Level.SSS, false, arrivals, TODAY, TERM).reached()).isFalse();
    }

    @Test
    void theStrictestReachedRuleWins() {
        var setting = with(rule("Warn at 1 a week", 1, Period.WEEK, LimitAction.WARN),
                rule("Refuse at 3 a term", 3, Period.TERM, LimitAction.REFUSE),
                rule("Reason at 2 a month", 2, Period.MONTH, LimitAction.REASON));
        var arrivals = List.of(at("2026-09-15T08:00:00Z"), at("2026-10-05T08:00:00Z"), at("2026-10-27T08:00:00Z"));
        var check = HealthCareLimits.check(setting, Level.JSS, false, arrivals, TODAY, TERM);
        assertThat(check.result()).isEqualTo(Result.REFUSE);
        assertThat(check.rule().name()).isEqualTo("Refuse at 3 a term");
    }

    @Test
    void termRulesDoNotApplyOutsideATerm() {
        var setting = with(rule("One a term", 1, Period.TERM, LimitAction.REFUSE));
        var check = HealthCareLimits.check(setting, Level.JSS, false, List.of(at("2026-10-27T08:00:00Z")), TODAY, null);
        assertThat(check.reached()).isFalse();
    }

    @Test
    void pausedRulesAreIgnored() {
        var setting = with(new LimitRule(null, "Paused", List.of(), 1, Period.WEEK, LimitAction.REFUSE, false));
        var check = HealthCareLimits.check(setting, Level.JSS, false, List.of(at("2026-10-27T08:00:00Z")), TODAY, TERM);
        assertThat(check.reached()).isFalse();
    }
}
