package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AssessmentConfiguration.PlanEntry;
import com.moriba.skultem.domain.service.ContinuousAssessmentCalculator;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaComponent;
import com.moriba.skultem.domain.vo.CaFrequency;

// A school names the parts of its continuous assessment and what each is worth: Test 1 = Attendance 20 +
// Assignment 20 + Class participation 20 + Test 40 (i.e. 10/10/10/20 of a term whose Test 1 is 50%).
class AssessmentConfigurationComponentsTest {

    private static final List<CaComponent> PARTS = List.of(new CaComponent("Attendance", 20),
            new CaComponent("Assignment", 20), new CaComponent("Class participation", 20));

    private AssessmentConfiguration config() {
        var c = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 60, 40,
                CaFrequency.WEEKLY, 6, "admin");
        c.replaceComponents(PARTS, "Test");
        return c;
    }

    @Test
    void componentsDecideTheRecordingsAndAreResolvedForAssessments() {
        var r = config().forAssessment("t1", "Test 1");

        assertThat(r.caEntries()).isEqualTo(3);
        assertThat(r.caFrequency()).isEqualTo(CaFrequency.CUSTOM);
        assertThat(r.components()).containsExactlyElementsOf(PARTS);
        assertThat(r.formalLabel()).isEqualTo("Test");
    }

    @Test
    void componentsMustAddUpToTheCaShare() {
        var c = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 50, 50,
                CaFrequency.WEEKLY, 6, "admin");

        assertThatThrownBy(() -> c.replaceComponents(PARTS, "Test")).isInstanceOf(RuleException.class)
                .hasMessageContaining("add up to 60%");
    }

    @Test
    void duplicateOrBlankNamesAreRejected() {
        var c = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 40, 60,
                CaFrequency.WEEKLY, 6, "admin");

        assertThatThrownBy(() -> c.replaceComponents(
                List.of(new CaComponent("Quiz", 20), new CaComponent("quiz", 20)), null))
                .isInstanceOf(RuleException.class).hasMessageContaining("twice");
        assertThatThrownBy(() -> c.replaceComponents(
                List.of(new CaComponent(" ", 40)), null)).isInstanceOf(RuleException.class);
    }

    @Test
    void aPlanEntryCanCarryItsOwnComponentsAndSplit() {
        var c = config();
        c.replacePlan(List.of(new PlanEntry("t2", "Test 2", true, 0,
                List.of(new CaComponent("Quiz", 15), new CaComponent("Project", 15)), "Exam")));

        var r = c.forAssessment("t2", "Test 2");
        assertThat(r.caPercentage()).isEqualTo(30);
        assertThat(r.formalPercentage()).isEqualTo(70);
        assertThat(r.components()).extracting(CaComponent::name).containsExactly("Quiz", "Project");
        assertThat(r.formalLabel()).isEqualTo("Exam");
        // everything else still follows the defaults
        assertThat(c.forAssessment("t1", "Test 1").components()).hasSize(3);
    }

    @Test
    void componentsSurviveStorage() {
        var c = config();
        c.replacePlan(List.of(new PlanEntry("t2", "Test 2", true, 0, List.of(new CaComponent("Quiz", 30)), "Exam")));

        var back = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 60, 40,
                CaFrequency.WEEKLY, 6, "admin");
        back.restoreComponents(c.componentsAsText(), c.getFormalLabel());
        back.restorePlan(c.planAsText());

        assertThat(back.getComponents()).containsExactlyElementsOf(PARTS);
        assertThat(back.forAssessment("t2", "Test 2").components()).containsExactly(new CaComponent("Quiz", 30));
    }

    @Test
    void theCaScoreIsTheWeightedAverageOfWhatIsRecorded() {
        var weights = List.of(20, 20, 20);

        // 80/100 attendance, 60 assignment, 70 participation -> 70
        assertThat(ContinuousAssessmentCalculator.caComponent(Map.of(1, 80, 2, 60, 3, 70), weights)).isEqualTo(70);
        // unequal weights: 100 at weight 10 and 40 at weight 30 -> 55
        assertThat(ContinuousAssessmentCalculator.caComponent(Map.of(1, 100, 2, 40), List.of(10, 30))).isEqualTo(55);
        // only some recorded so far
        assertThat(ContinuousAssessmentCalculator.caComponent(Map.of(2, 60), weights)).isEqualTo(60);
        assertThat(ContinuousAssessmentCalculator.caComponent(Map.of(), weights)).isNull();
        // no weights = plain average, exactly as before
        assertThat(ContinuousAssessmentCalculator.caComponent(Map.of(1, 80, 2, 60), List.of())).isEqualTo(70);
    }

    @Test
    void aTemplatesOwnSetupAppliesCaOnlyToTheTestsItLists() {
        var c = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin").forTemplate("tpl");
        c.replacePlan(List.of(new PlanEntry(AssessmentConfiguration.ALL_TERMS, "Test 1", true, 0, PARTS, "Test")));

        var test1 = c.forAssessment("t1", "Test 1");
        assertThat(test1.structure()).isEqualTo(AssessmentStructure.CA_AND_TEST);
        assertThat(test1.components()).containsExactlyElementsOf(PARTS);
        assertThat(test1.caPercentage()).isEqualTo(60);
        assertThat(test1.formalPercentage()).isEqualTo(40);
        // any term, so a new term needs no extra setup
        assertThat(c.forAssessment("a-new-term", "test 1").structure()).isEqualTo(AssessmentStructure.CA_AND_TEST);
        // the exam, or a test added later, is a single score
        assertThat(c.forAssessment("t1", "Exam").structure()).isEqualTo(AssessmentStructure.SIMPLE);
    }

    @Test
    void aTermSpecificEntryWinsOverTheAllTermsOne() {
        var c = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin").forTemplate("tpl");
        c.replacePlan(List.of(
                new PlanEntry(AssessmentConfiguration.ALL_TERMS, "Test 1", true, 0, PARTS, "Test"),
                new PlanEntry("t3", "Test 1", false, 0)));

        assertThat(c.forAssessment("t3", "Test 1").structure()).isEqualTo(AssessmentStructure.SIMPLE);
        assertThat(c.forAssessment("t1", "Test 1").structure()).isEqualTo(AssessmentStructure.CA_AND_TEST);
    }
}
