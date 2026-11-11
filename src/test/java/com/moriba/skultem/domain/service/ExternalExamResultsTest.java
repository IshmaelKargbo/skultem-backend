package com.moriba.skultem.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.ExternalExamDefaults;
import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.model.ExternalExamSitting;
import com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject;
import com.moriba.skultem.domain.vo.ExamGrade;

class ExternalExamResultsTest {

    private static ExternalExam builtIn(String code) {
        var d = ExternalExamDefaults.all().stream().filter(x -> x.code().equals(code)).findFirst().orElseThrow();
        return ExternalExam.create("e", "s", d.code(), d.name(), d.examBody(), d.level(), d.scale(), d.mode(),
                d.bestOf(), true);
    }

    private static List<ExamSubject> subjects(int n) {
        return java.util.stream.IntStream.rangeClosed(1, n).mapToObj(i -> new ExamSubject("s" + i, "Subject " + i))
                .toList();
    }

    @Test
    void wassceGradesOnTheWaecScaleAndAddsTheBestSix() {
        var exam = builtIn("WASSCE");
        // A1(1) B2(2) B3(3) C4(4) C5(5) C6(6) F9(9): the best six add to 21, F9 is left out.
        Map<String, Double> scores = new LinkedHashMap<>();
        double[] values = { 80, 72, 66, 61, 57, 50, 20 };
        for (int i = 0; i < values.length; i++) {
            scores.put("s" + (i + 1), values[i]);
        }
        var r = ExternalExamResults.of(exam, subjects(7), scores);

        assertThat(r.subjects()).extracting(ExternalExamResults.SubjectResult::grade)
                .containsExactly("A1", "B2", "B3", "C4", "C5", "C6", "F9");
        assertThat(r.aggregate()).isEqualTo(21);
        assertThat(r.aggregateLabel()).isEqualTo("Aggregate (best 6)");
        assertThat(r.subjectsSat()).isEqualTo(7);
    }

    @Test
    void anUnsatSubjectIsAbsentAndDoesNotCount() {
        var exam = builtIn("BECE");
        var r = ExternalExamResults.of(exam, subjects(2), Map.of("s1", 78.0));

        assertThat(r.subjects().get(1).grade()).isEqualTo("ABS");
        assertThat(r.subjects().get(1).absent()).isTrue();
        assertThat(r.aggregate()).isEqualTo(1);
        assertThat(r.subjectsSat()).isEqualTo(1);
    }

    @Test
    void npseAddsUpTheScores() {
        var exam = builtIn("NPSE");
        var r = ExternalExamResults.of(exam, subjects(4), Map.of("s1", 80.0, "s2", 70.5, "s3", 60.0, "s4", 50.0));

        assertThat(r.aggregate()).isEqualTo(261);
        assertThat(r.aggregateLabel()).isEqualTo("Total score");
    }

    @Test
    void noScoresMeansNoAggregate() {
        assertThat(ExternalExamResults.of(builtIn("BECE"), subjects(3), Map.of()).aggregate()).isNull();
    }

    @Test
    void aScaleMustNotOverlapOrLeaveTheRange() {
        assertThatThrownBy(() -> ExternalExam.create("e", "s", "X", "X", null, null, List.of(
                new ExamGrade(50, 100, "A", 1, ""), new ExamGrade(0, 50, "B", 2, "")),
                ExternalExam.AggregateMode.POINTS, 0, false))
                .isInstanceOf(RuleException.class).hasMessageContaining("overlap");
        assertThatThrownBy(() -> ExternalExam.create("e", "s", "X", "X", null, null, List.of(
                new ExamGrade(0, 120, "A", 1, "")), ExternalExam.AggregateMode.POINTS, 0, false))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> ExternalExam.create("e", "s", " ", "X", null, null,
                List.of(new ExamGrade(0, 100, "A", 1, "")), ExternalExam.AggregateMode.POINTS, 0, false))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void theBuiltInScalesAreValid() {
        for (var d : ExternalExamDefaults.all()) {
            assertThat(builtIn(d.code()).getGradeScale()).isNotEmpty();
        }
    }

    @Test
    void confirmedResultsAreLockedUntilReopened() {
        var sitting = ExternalExamSitting.create("x", "s", "e", "c", "y", null, subjects(2));
        sitting.confirm("Mrs. Kamara");

        assertThat(sitting.isConfirmed()).isTrue();
        assertThatThrownBy(() -> sitting.edit("1", subjects(1))).isInstanceOf(RuleException.class)
                .hasMessageContaining("Reopen");
        sitting.reopen();
        assertThat(sitting.isConfirmed()).isFalse();
        assertThat(sitting.getConfirmedByName()).isNull();
    }

    @Test
    void beceResultsAreEnteredAsGradesOneToSeven() {
        var bece = builtIn("BECE").withEntryMode(ExternalExam.EntryMode.GRADES);
        var c = ExternalExamCandidate.create("c", "s", "x", "st", "Aminata Kamara", "S-1");
        Set<String> ids = Set.of("s1", "s2", "s3", "s4", "s5", "s6", "s7");
        Map<String, String> grades = new LinkedHashMap<>();
        grades.put("s1", "1");
        grades.put("s2", "2");
        grades.put("s3", "2");
        grades.put("s4", "3");
        grades.put("s5", "4");
        grades.put("s6", "1");
        grades.put("s7", "7");
        c.recordGrades("1020304-001", grades, ids, g -> bece.band(g) == null ? null : bece.band(g).grade());

        var r = ExternalExamResults.of(bece, subjects(7), c.getScores(), c.getGrades());
        // Best 6 of 1,2,2,3,4,1,7: the 7 is left out.
        assertThat(r.aggregate()).isEqualTo(13);
        assertThat(r.subjects().get(0).remark()).isEqualTo("Excellent");
        assertThat(r.subjects().get(6).points()).isEqualTo(7);
        assertThat(r.subjectsSat()).isEqualTo(7);
        assertThat(c.hasResults()).isTrue();
    }

    @Test
    void withGradesASubjectNotTakenIsLeftOffAndAbsIsAbsent() {
        var wassce = builtIn("WASSCE").withEntryMode(ExternalExam.EntryMode.GRADES);
        // An Art student: no Physics (not taken), absent from Economics.
        var r = ExternalExamResults.of(wassce, subjects(3), Map.of(), Map.of("s1", "B2", "s3", "ABS"));

        assertThat(r.subjects()).extracting(ExternalExamResults.SubjectResult::subjectId).containsExactly("s1", "s3");
        assertThat(r.subjects().get(1).absent()).isTrue();
        assertThat(r.subjectsSat()).isEqualTo(1);
        assertThat(r.aggregate()).isEqualTo(2);
    }

    @Test
    void aGradeTheExamDoesNotHaveIsRefused() {
        var bece = builtIn("BECE").withEntryMode(ExternalExam.EntryMode.GRADES);
        var c = ExternalExamCandidate.create("c", "s", "x", "st", "Aminata Kamara", "S-1");
        assertThatThrownBy(() -> c.recordGrades("1", Map.of("s1", "9"), Set.of("s1"),
                g -> bece.band(g) == null ? null : bece.band(g).grade()))
                .isInstanceOf(RuleException.class).hasMessageContaining("not one of this exam's grades");
        // WASSCE grades are matched whatever the case.
        var wassce = builtIn("WASSCE").withEntryMode(ExternalExam.EntryMode.GRADES);
        c.recordGrades("1", Map.of("s1", "b3"), Set.of("s1"), g -> wassce.band(g) == null ? null : wassce.band(g).grade());
        assertThat(c.getGrades()).containsEntry("s1", "B3");
    }

    @Test
    void scoresAreCheckedWhenRecorded() {
        var c = ExternalExamCandidate.create("c", "s", "x", "st", "Aminata Bangura", "S-1");
        Set<String> ids = Set.of("s1", "s2");

        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("s1", 64.0);
        scores.put("s2", null);
        c.record(" 1020304-001 ", scores, ids);
        assertThat(c.getIndexNumber()).isEqualTo("1020304-001");
        assertThat(c.getScores()).containsOnlyKeys("s1");

        assertThatThrownBy(() -> c.record("1", Map.of("s1", 101.0), ids)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> c.record("1", Map.of("other", 50.0), ids)).isInstanceOf(RuleException.class);
    }
}
