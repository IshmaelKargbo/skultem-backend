package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AssessmentConfiguration.PlanEntry;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

// Terms and assessments differ in length: Test 1 in Term 1 might have 6 weeks of CA, Test 1 in the shorter Term 3 only
// 3, and the Exam none at all. The school says so per term and per assessment; each assessment gets its own.
class AssessmentConfigurationPlanTest {

    private AssessmentConfiguration config;

    @BeforeEach
    void setUp() {
        config = AssessmentConfiguration.create("school", "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin");
        config.replacePlan(List.of(
                new PlanEntry("t3", "Test 1", true, 3),
                new PlanEntry("t1", "Exam", false, 0)));
    }

    @Test
    void anAssessmentWithoutAnEntryFollowsTheDefaults() {
        var r = config.forAssessment("t1", "Test 1");

        assertThat(r.structure()).isEqualTo(AssessmentStructure.CA_AND_TEST);
        assertThat(r.caEntries()).isEqualTo(6);
    }

    @Test
    void aShorterTermGetsItsOwnNumberOfWeeks() {
        var r = config.forAssessment("t3", "test 1");

        assertThat(r.caEntries()).isEqualTo(3);
        assertThat(r.caPercentage()).isEqualTo(30);
    }

    @Test
    void anAssessmentThePlanMarksAsNoCaStaysASingleScore() {
        var r = config.forAssessment("t1", "Exam");

        assertThat(r.structure()).isEqualTo(AssessmentStructure.SIMPLE);
        assertThat(r.caEntries()).isZero();
        assertThat(r.formalPercentage()).isEqualTo(100);
    }

    @Test
    void anAssessmentFreezesItsOwnTermAndNameOnOpening() {
        var term = mock(Term.class);
        when(term.getId()).thenReturn("t3");
        var assessment = mock(Assessment.class);
        when(assessment.getName()).thenReturn("Test 1");
        var cycle = ClassSubjectAssessmentLifeCycle.create("c", "school", mock(TeacherSubject.class), term, assessment,
                Status.DRAFT);

        cycle.freezeStructure(config);

        assertThat(cycle.isContinuous()).isTrue();
        assertThat(cycle.getCaEntries()).isEqualTo(3);
    }

    @Test
    void resyncingReportsWhetherAnythingChangedAndLeavesAnAlreadyRightAssessmentAlone() {
        var term = mock(Term.class);
        when(term.getId()).thenReturn("t1");
        var assessment = mock(Assessment.class);
        when(assessment.getName()).thenReturn("Exam");
        var cycle = ClassSubjectAssessmentLifeCycle.create("c", "school", mock(TeacherSubject.class), term, assessment,
                Status.LOCKED);
        cycle.freezeStructure(AssessmentConfiguration.simple("school", "sec"));

        assertThat(cycle.resyncStructure(config)).isFalse();
        assertThat(cycle.isContinuous()).isFalse();
    }

    @Test
    void thePlanSurvivesTheRoundTripToStorage() {
        var restored = AssessmentConfiguration.create("school", "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin");
        restored.restorePlan(config.planAsText());

        assertThat(restored.getPlan()).containsExactlyElementsOf(config.getPlan());
    }

    @Test
    void aPlanIsDroppedWhenTheSectionGoesBackToSimple() {
        config.change(AssessmentStructure.SIMPLE, 0, 100, null, 0, "admin");

        assertThat(config.getPlan()).isEmpty();
        assertThat(config.forAssessment("t3", "Test 1").structure()).isEqualTo(AssessmentStructure.SIMPLE);
    }

    @Test
    void badPlanEntriesAreRejected() {
        assertThatThrownBy(() -> config.replacePlan(List.of(new PlanEntry("t1", "Test 1", true, 0))))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> config.replacePlan(List.of(new PlanEntry("t1", "Test 1", true, 4),
                new PlanEntry("t1", "test 1", true, 5)))).isInstanceOf(RuleException.class)
                .hasMessageContaining("twice");
    }
}
