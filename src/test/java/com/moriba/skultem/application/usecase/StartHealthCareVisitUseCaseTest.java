package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.HealthCareSupport.CurrentStudent;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitAction;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;
import com.moriba.skultem.domain.model.HealthCareSetting.Period;
import com.moriba.skultem.domain.model.HealthCareTurnedAway;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.HealthCareTurnedAwayRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.service.HealthCareLimits.TermDates;

// docs/HEALTH_CARE.md §6.2 acceptance criteria 11-12: Refuse records a turned-away entry, urgent goes through,
// "Ask for a reason" needs one, and a continuation of today's visit skips the limits.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartHealthCareVisitUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");
    private static final Instant MONDAY_VISIT = Instant.parse("2026-10-26T09:00:00Z");

    @Mock
    private HealthCareVisitRepository visitRepo;
    @Mock
    private HealthCareTurnedAwayRepository turnedAwayRepo;
    @Mock
    private HealthCareSupport support;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private StartHealthCareVisitUseCase useCase;

    private HealthCareSetting setting(LimitAction action) {
        var s = HealthCareSetting.defaults("school");
        var d = HealthCareSetting.defaultValues();
        s.update(new HealthCareSetting.Values(List.of(new LimitRule(null, "One a week", List.of(), 1, Period.WEEK,
                action, true)), d.medicationAllowed(), d.sameMedicineHours(), d.sameMedicineAction(),
                d.maxMedicationLines(), d.requireBroughtBy(), d.requireTemperature(), d.requireTreatment(),
                d.requireNotesOnReturn(), d.notifyOutcomes(), d.allowManualNotify(), d.notifyPrincipalOnReferral(),
                d.principalAlertVisits(), d.principalAlertPeriod(), d.longStayMinutes(), d.complaints()), "p", "P", NOW);
        return s;
    }

    @BeforeEach
    void setUp() {
        var student = mock(Student.class);
        when(student.getName()).thenReturn("Aminata Kamara");
        var enrollment = mock(Enrollment.class);
        when(support.currentStudent("school", "s1")).thenReturn(new CurrentStudent(student, enrollment, "JSS 2 A"));
        when(support.now()).thenReturn(NOW);
        when(support.today()).thenReturn(TODAY);
        when(support.userName("nurse")).thenReturn("Nurse Bangura");
        when(support.termOn(any(), any())).thenReturn(new TermDates(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 12, 18)));
        when(support.countingSince(any(), any())).thenReturn(Instant.parse("2026-09-07T00:00:00Z"));
        when(support.countedArrivals(eq("school"), eq("s1"), any())).thenReturn(List.of(MONDAY_VISIT));
        when(support.checkLimits(any(), any(), anyBoolean(), anyList(), any(), any())).thenCallRealMethod();
        when(support.schoolDayEnd(any(), any(), any())).thenReturn(Instant.parse("2026-10-28T15:00:00Z"));
        when(visitRepo.findOpenByStudent("school", "s1")).thenReturn(Optional.empty());
    }

    private static StartHealthCareVisitUseCase.Command command(String complaint, String reason, String continues) {
        return new StartHealthCareVisitUseCase.Command("s1", null, List.of(complaint), null, null, null, null, continues,
                reason);
    }

    @Test
    void aRefuseRuleRecordsATurnedAwayEntryInsteadOfAVisit() {
        when(support.settings("school")).thenReturn(setting(LimitAction.REFUSE));

        var res = useCase.execute("school", command("HEADACHE", null, null), "nurse");

        assertThat(res.turnedAway()).isTrue();
        assertThat(res.visit()).isNull();
        var captor = ArgumentCaptor.forClass(HealthCareTurnedAway.class);
        verify(turnedAwayRepo).save(captor.capture());
        assertThat(captor.getValue().getRuleName()).isEqualTo("One a week");
        assertThat(captor.getValue().getVisitCount()).isEqualTo(1);
        verify(visitRepo, never()).save(any());
    }

    @Test
    void anUrgentComplaintGoesThroughAndIsFlaggedOverTheLimit() {
        when(support.settings("school")).thenReturn(setting(LimitAction.REFUSE));

        var res = useCase.execute("school", command("BREATHING", null, null), "nurse");

        assertThat(res.turnedAway()).isFalse();
        var captor = ArgumentCaptor.forClass(HealthCareVisit.class);
        verify(visitRepo).save(captor.capture());
        var overLimit = captor.getValue().getOverLimit();
        assertThat(overLimit).isNotNull();
        assertThat(overLimit.urgent()).isTrue();
        assertThat(overLimit.ruleName()).isEqualTo("One a week");
        verify(turnedAwayRepo, never()).save(any());
    }

    @Test
    void askForAReasonRefusesWithoutOneAndKeepsItWhenGiven() {
        when(support.settings("school")).thenReturn(setting(LimitAction.REASON));

        assertThatThrownBy(() -> useCase.execute("school", command("HEADACHE", null, null), "nurse"))
                .isInstanceOf(RuleException.class).hasMessageContaining("reason");

        useCase.execute("school", command("HEADACHE", "Feverish, teacher asked", null), "nurse");
        var captor = ArgumentCaptor.forClass(HealthCareVisit.class);
        verify(visitRepo).save(captor.capture());
        assertThat(captor.getValue().getOverLimit().reason()).isEqualTo("Feverish, teacher asked");
        assertThat(captor.getValue().getOverLimit().by()).isEqualTo("nurse");
    }

    @Test
    void continuingTodaysVisitSkipsTheLimits() {
        when(support.settings("school")).thenReturn(setting(LimitAction.REFUSE));
        var earlier = HealthCareVisit.start("school", "s1", "Aminata Kamara", "JSS 2 A",
                Instant.parse("2026-10-28T08:00:00Z"), Instant.parse("2026-10-28T15:00:00Z"),
                List.of(new HealthCareVisit.Complaint("HEADACHE", "Headache", false)), null, null, null, null, null,
                null, "nurse", "Nurse", Instant.parse("2026-10-28T08:00:00Z"));
        earlier.close(new HealthCareVisit.Closing(HealthCareVisit.Outcome.RETURNED_TO_CLASS, null, "Rest", null, null,
                null, null, null, null), true, false, "nurse", "Nurse", Instant.parse("2026-10-28T08:30:00Z"));
        when(visitRepo.findByIdAndSchool(earlier.getId(), "school")).thenReturn(Optional.of(earlier));

        var res = useCase.execute("school", command("HEADACHE", null, earlier.getId()), "nurse");

        assertThat(res.turnedAway()).isFalse();
        var captor = ArgumentCaptor.forClass(HealthCareVisit.class);
        verify(visitRepo).save(captor.capture());
        assertThat(captor.getValue().getContinuesVisitId()).isEqualTo(earlier.getId());
        assertThat(captor.getValue().getOverLimit()).isNull();
    }

    @Test
    void aStudentAlreadyInTheSickBayCannotStartASecondVisit() {
        when(support.settings("school")).thenReturn(setting(LimitAction.WARN));
        var open = mock(HealthCareVisit.class);
        when(visitRepo.findOpenByStudent("school", "s1")).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> useCase.execute("school", command("HEADACHE", null, null), "nurse"))
                .isInstanceOf(RuleException.class).hasMessageContaining("already in the sick bay");
        verify(turnedAwayRepo, never()).save(any());
    }

    @Test
    void aHiddenComplaintCannotBeChosen() {
        var s = HealthCareSetting.defaults("school");
        var d = HealthCareSetting.defaultValues();
        s.update(new HealthCareSetting.Values(List.of(), d.medicationAllowed(), d.sameMedicineHours(),
                d.sameMedicineAction(), d.maxMedicationLines(), d.requireBroughtBy(), d.requireTemperature(),
                d.requireTreatment(), d.requireNotesOnReturn(), d.notifyOutcomes(), d.allowManualNotify(),
                d.notifyPrincipalOnReferral(), d.principalAlertVisits(), d.principalAlertPeriod(), d.longStayMinutes(),
                new HealthCareSetting.ComplaintSettings(List.of("TOOTHACHE"), List.of(), List.of())), "p", "P", NOW);
        when(support.settings("school")).thenReturn(s);

        assertThatThrownBy(() -> useCase.execute("school", command("TOOTHACHE", null, null), "nurse"))
                .isInstanceOf(RuleException.class).hasMessageContaining("no longer");
    }
}
