package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.service.TermWeights;

// A template can sit Test 1 and Test 2 in terms 1-2 and only an Exam in term 3; the usual template runs every test
// in every term.
class AssessmentTermsTest {

    private final AssessmentTemplate template = AssessmentTemplate.create("t", "s", "KCS", "d", 50);

    private Assessment test(String name, int weight, int position, Set<Integer> terms) {
        return Assessment.create(name, "s", template, name, weight, position, terms);
    }

    @Test
    void aTestRunsInEveryTermUnlessLimited() {
        var plain = Assessment.create("a", "s", template, "Test 1", 20, 1);
        assertThat(plain.getTerms()).isEmpty();
        assertThat(plain.runsInTerm(1)).isTrue();
        assertThat(plain.runsInTerm(3)).isTrue();

        var exam = test("Exam", 100, 3, Set.of(3));
        assertThat(exam.runsInTerm(1)).isFalse();
        assertThat(exam.runsInTerm(3)).isTrue();
    }

    @Test
    void eachTermGetsOnlyItsOwnTestsInOrder() {
        var all = List.of(test("Exam", 100, 3, Set.of(3)), test("Test 2", 50, 2, Set.of(1, 2)),
                test("Test 1", 50, 1, Set.of(1, 2)));

        assertThat(Assessment.forTerm(all, 1)).extracting(Assessment::getName).containsExactly("Test 1", "Test 2");
        assertThat(Assessment.forTerm(all, 3)).extracting(Assessment::getName).containsExactly("Exam");
        assertThat(Assessment.weightInTerm(all, 2)).isEqualTo(100);
        assertThat(Assessment.weightInTerm(all, 3)).isEqualTo(100);
    }

    @Test
    void termsAreStoredAsAShortListAndEmptyMeansEveryTerm() {
        assertThat(Assessment.formatTerms(Set.of(2, 1))).isEqualTo("1,2");
        assertThat(Assessment.formatTerms(Set.of())).isNull();
        assertThat(Assessment.parseTerms("1,2")).containsExactly(1, 2);
        assertThat(Assessment.parseTerms(null)).isEmpty();
    }

    @Test
    void aTermOutsideTheYearIsRefused() {
        assertThatThrownBy(() -> test("Exam", 100, 1, Set.of(0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weightsAreCheckedTermByTermWhenTestsAreLimited() {
        // Test 1 + Test 2 in terms 1-2, Exam alone in term 3: 100% each term, though the tests add up to 200%.
        assertThat(TermWeights.overweight(List.of(
                new TermWeights.Line("Test 1", 50, Set.of(1, 2)),
                new TermWeights.Line("Test 2", 50, Set.of(1, 2)),
                new TermWeights.Line("Exam", 100, Set.of(3))))).isNull();

        assertThat(TermWeights.overweight(List.of(
                new TermWeights.Line("Test 1", 50, Set.of()),
                new TermWeights.Line("Exam", 100, Set.of(3))))).contains("term 3");
    }

    @Test
    void theUsualTemplateKeepsThePlainTotal() {
        assertThat(TermWeights.overweight(List.of(
                new TermWeights.Line("Test 1", 20, Set.of()),
                new TermWeights.Line("Test 2", 20, Set.of()),
                new TermWeights.Line("Exam", 60, Set.of())))).isNull();
        assertThat(TermWeights.overweight(List.of(
                new TermWeights.Line("Test 1", 60, Set.of()),
                new TermWeights.Line("Exam", 60, Set.of())))).isEqualTo("Total assessment weight cannot exceed 100%");
    }
}
