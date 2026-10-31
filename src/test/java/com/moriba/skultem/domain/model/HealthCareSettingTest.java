package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.HealthCareSetting.ComplaintSettings;
import com.moriba.skultem.domain.model.HealthCareSetting.CustomComplaint;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitAction;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;
import com.moriba.skultem.domain.model.HealthCareSetting.Period;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;

// docs/HEALTH_CARE.md §6: defaults, validation, and the complaint list rules (C-1, C-2).
class HealthCareSettingTest {

    private static HealthCareSetting.Values values(List<LimitRule> rules, ComplaintSettings complaints) {
        var d = HealthCareSetting.defaultValues();
        return new HealthCareSetting.Values(rules, d.medicationAllowed(), d.sameMedicineHours(), d.sameMedicineAction(),
                d.maxMedicationLines(), d.requireBroughtBy(), d.requireTemperature(), d.requireTreatment(),
                d.requireNotesOnReturn(), d.notifyOutcomes(), d.allowManualNotify(), d.notifyPrincipalOnReferral(),
                d.principalAlertVisits(), d.principalAlertPeriod(), d.longStayMinutes(), complaints);
    }

    @Test
    void defaultsMatchTheSpec() {
        var s = HealthCareSetting.defaults("school");
        assertThat(s.getLimitRules()).isEmpty();
        assertThat(s.isMedicationAllowed()).isTrue();
        assertThat(s.getSameMedicineHours()).isEqualTo(4);
        assertThat(s.isRequireTreatment()).isTrue();
        assertThat(s.notifiesParentsFor(Outcome.SENT_HOME)).isTrue();
        assertThat(s.notifiesParentsFor(Outcome.REFERRED)).isTrue();
        assertThat(s.notifiesParentsFor(Outcome.RETURNED_TO_CLASS)).isFalse();
        assertThat(s.getLongStayMinutes()).isEqualTo(60);
        assertThat(s.complaintList()).hasSize(HealthCareComplaint.STANDARD.size());
    }

    @Test
    void rulesAreValidatedAndGivenIds() {
        var s = HealthCareSetting.defaults("school");
        assertThatThrownBy(() -> s.update(values(List.of(new LimitRule(null, "x", List.of(), 0, Period.WEEK,
                LimitAction.WARN, true)), null), "p", "P", Instant.now())).hasMessageContaining("between 1 and 50");
        s.update(values(List.of(new LimitRule(null, " One a week ", null, 1, Period.WEEK, LimitAction.REFUSE, true)),
                null), "p", "P", Instant.now());
        assertThat(s.getLimitRules().get(0).id()).isNotBlank();
        assertThat(s.getLimitRules().get(0).name()).isEqualTo("One a week");
    }

    @Test
    void lockedUrgentComplaintsAndOtherCannotBeHidden() {
        var s = HealthCareSetting.defaults("school");
        assertThatThrownBy(() -> s.update(values(List.of(), new ComplaintSettings(List.of("BREATHING"), List.of(),
                List.of())), "p", "P", Instant.now())).hasMessageContaining("can't be hidden");
        assertThatThrownBy(() -> s.update(values(List.of(), new ComplaintSettings(List.of("OTHER"), List.of(),
                List.of())), "p", "P", Instant.now())).hasMessageContaining("can't be hidden");
    }

    @Test
    void schoolsCanHideAddAndMarkComplaintsUrgent() {
        var s = HealthCareSetting.defaults("school");
        s.update(values(List.of(), new ComplaintSettings(List.of("TOOTHACHE"), List.of("FEVER"),
                List.of(new CustomComplaint(null, "Asthma attack", true, false)))), "p", "P", Instant.now());
        assertThat(s.complaint("TOOTHACHE").orElseThrow().hidden()).isTrue();
        assertThat(s.complaint("FEVER").orElseThrow().urgent()).isTrue();
        assertThat(s.complaint("BREATHING").orElseThrow().urgent()).isTrue();
        var custom = s.complaintList().stream().filter(c -> !c.standard()).findFirst().orElseThrow();
        assertThat(custom.label()).isEqualTo("Asthma attack");
        assertThat(custom.code()).startsWith("CUSTOM_");
        assertThatThrownBy(() -> s.update(values(List.of(), new ComplaintSettings(List.of(), List.of(),
                List.of(new CustomComplaint(null, "headache", false, false)))), "p", "P", Instant.now()))
                .hasMessageContaining("already on the complaint list");
    }
}
