package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

// A completed CA week is locked so the record can't be quietly changed; only an administrator's unlock reopens it.
class ClassSubjectAssessmentLifeCycleWeekLockTest {

    private ClassSubjectAssessmentLifeCycle cycle;

    @BeforeEach
    void setUp() {
        cycle = ClassSubjectAssessmentLifeCycle.create("c1", "school", mock(TeacherSubject.class), mock(Term.class),
                mock(Assessment.class), Status.DRAFT);
        cycle.freezeStructure(AssessmentConfiguration.create("school", "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin"));
    }

    @Test
    void aLockedWeekStaysLockedAndCannotBeLockedTwice() {
        cycle.lockWeek(2);

        assertThat(cycle.isWeekLocked(2)).isTrue();
        assertThat(cycle.isWeekLocked(1)).isFalse();
        assertThatThrownBy(() -> cycle.lockWeek(2)).isInstanceOf(RuleException.class).hasMessageContaining("already locked");
    }

    @Test
    void thereIsNoWeekBeyondTheNumberTheAssessmentWasOpenedWith() {
        assertThatThrownBy(() -> cycle.lockWeek(7)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> cycle.lockWeek(0)).isInstanceOf(RuleException.class);
    }

    @Test
    void submittingTheCaLocksEveryWeek() {
        cycle.submitCa();

        assertThat(cycle.getCaLockedWeeks()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void unlockingAWeekAfterTheCaWasSubmittedReopensTheCa() {
        cycle.submitCa();

        cycle.unlockWeek(3);

        assertThat(cycle.isWeekLocked(3)).isFalse();
        assertThat(cycle.isCaSubmitted()).isFalse();
        assertThat(cycle.getCaLockedWeeks()).containsExactly(1, 2, 4, 5, 6);
    }

    @Test
    void anUnlockedWeekCannotBeUnlockedAgain() {
        assertThatThrownBy(() -> cycle.unlockWeek(1)).isInstanceOf(RuleException.class).hasMessageContaining("not locked");
    }

    @Test
    void lockedWeeksSurviveTheRoundTripToStorage() {
        cycle.lockWeek(1);
        cycle.lockWeek(3);

        var text = cycle.lockedWeeksAsText();
        var restored = ClassSubjectAssessmentLifeCycle.create("c2", "school", mock(TeacherSubject.class), mock(Term.class),
                mock(Assessment.class), Status.DRAFT);
        restored.restoreLockedWeeks(text);

        assertThat(text).isEqualTo("1,3");
        assertThat(restored.getCaLockedWeeks()).containsExactly(1, 3);
    }

    @Test
    void nothingCanBeLockedOnceItHasGoneForApproval() {
        cycle.submit();

        assertThatThrownBy(() -> cycle.lockWeek(1)).isInstanceOf(RuleException.class);
    }
}
