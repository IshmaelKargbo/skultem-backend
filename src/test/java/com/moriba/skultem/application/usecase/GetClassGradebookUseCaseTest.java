package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.usecase.GetClassGradebookUseCase.Line;

// The grade book's assembly: what's shown, how totals, averages and positions are worked out.
class GetClassGradebookUseCaseTest {

    private static Line line(String ts, String subject, String a, String aName, int pos, int weight, String status,
            String pupil, String name, Integer score) {
        return new Line(ts, subject, subject.substring(0, 3).toUpperCase(), "Mr. Kamara", a, aName, pos, weight,
                status, pupil, name, "ADM-" + pupil, null, score);
    }

    private static String grade(int score) {
        return score >= 70 ? "A" : score >= 50 ? "C" : "F";
    }

    private static final List<Line> LINES = List.of(
            // Maths: Test 1 (30%) approved, Exam (70%) awaiting approval.
            line("ts-m", "Mathematics", "t1", "Test 1", 1, 30, "APPROVED", "p1", "Aminata Kamara", 80),
            line("ts-m", "Mathematics", "ex", "Exam", 2, 70, "SUBMITTED", "p1", "Aminata Kamara", 60),
            line("ts-m", "Mathematics", "t1", "Test 1", 1, 30, "APPROVED", "p2", "Mohamed Sesay", 40),
            line("ts-m", "Mathematics", "ex", "Exam", 2, 70, "SUBMITTED", "p2", "Mohamed Sesay", 50),
            // English: Test 1 still a draft - its marks stay the teacher's.
            line("ts-e", "English", "t1", "Test 1", 1, 30, "DRAFT", "p1", "Aminata Kamara", 95),
            line("ts-e", "English", "t1", "Test 1", 1, 30, "DRAFT", "p2", "Mohamed Sesay", 10),
            // Science: approved, and a returned one that counts as not submitted.
            line("ts-s", "Science", "t1", "Test 1", 1, 50, "COMPLETED", "p1", "Aminata Kamara", 70),
            line("ts-s", "Science", "ex", "Exam", 2, 50, "RETURNED", "p1", "Aminata Kamara", 20),
            line("ts-s", "Science", "t1", "Test 1", 1, 50, "COMPLETED", "p2", "Mohamed Sesay", 90),
            line("ts-s", "Science", "ex", "Exam", 2, 50, "RETURNED", "p2", "Mohamed Sesay", 20));

    @Test
    void showsOnlySubmittedMarksWithWeightedTotals() {
        var g = GetClassGradebookUseCase.assemble("s1", "JSS 2 · A", "t", "Second Term", 50, LINES,
                GetClassGradebookUseCaseTest::grade);

        var aminata = g.students().stream().filter(s -> s.studentId().equals("p1")).findFirst().orElseThrow();
        // Maths: (80×30 + 60×70) / 100 = 66.
        assertThat(aminata.subjects().get("ts-m").total()).isEqualTo(66.0);
        assertThat(aminata.subjects().get("ts-m").grade()).isEqualTo("C");
        assertThat(aminata.subjects().get("ts-m").scores()).containsEntry("t1", 80).containsEntry("ex", 60);
        // English is a draft: nothing shown, no total.
        assertThat(aminata.subjects().get("ts-e").scores()).isEmpty();
        assertThat(aminata.subjects().get("ts-e").total()).isNull();
        // Science: only the completed test counts; the returned exam is hidden.
        assertThat(aminata.subjects().get("ts-s").scores()).containsOnlyKeys("t1");
        assertThat(aminata.subjects().get("ts-s").total()).isEqualTo(70.0);
        // Average over the subjects with a total: (66 + 70) / 2.
        assertThat(aminata.average()).isEqualTo(68.0);
        assertThat(aminata.passed()).isTrue();
    }

    @Test
    void subjectsCarryEachAssessmentsStatusAndHowManyAreIn() {
        var g = GetClassGradebookUseCase.assemble("s1", "JSS 2 · A", "t", "Second Term", 50, LINES,
                GetClassGradebookUseCaseTest::grade);

        assertThat(g.subjects()).extracting(s -> s.name()).containsExactly("Mathematics", "English", "Science");
        assertThat(g.subjects()).extracting(s -> s.code()).containsExactly("MAT", "ENG", "SCI");
        var maths = g.subjects().get(0);
        assertThat(maths.assessments()).extracting(a -> a.name() + ":" + a.status())
                .containsExactly("Test 1:APPROVED", "Exam:SUBMITTED");
        assertThat(maths.submitted()).isEqualTo(2);
        // (66 + 47) / 2 = 56.5 - Mohamed's maths is (40×30 + 50×70) / 100 = 47.
        assertThat(maths.classAverage()).isEqualTo(56.5);
        var english = g.subjects().get(1);
        assertThat(english.submitted()).isZero();
        assertThat(english.classAverage()).isNull();
        assertThat(g.subjects().get(2).assessments()).extracting(a -> a.status())
                .containsExactly("APPROVED", "NOT_SUBMITTED");
    }

    @Test
    void positionsByAverageAndTiesShareAPlace() {
        var g = GetClassGradebookUseCase.assemble("s1", "JSS 2 · A", "t", "Second Term", 50, LINES,
                GetClassGradebookUseCaseTest::grade);
        // Aminata 68.0, Mohamed (47 + 90) / 2 = 68.5.
        assertThat(g.students()).extracting(s -> s.name() + "#" + s.position())
                .containsExactly("Mohamed Sesay#1", "Aminata Kamara#2");

        var tied = GetClassGradebookUseCase.assemble("s1", "X", "t", "T", 50, List.of(
                line("ts", "Maths", "t1", "Test", 1, 100, "APPROVED", "a", "Ada", 60),
                line("ts", "Maths", "t1", "Test", 1, 100, "APPROVED", "b", "Ben", 60),
                line("ts", "Maths", "t1", "Test", 1, 100, "APPROVED", "c", "Cal", 40),
                line("ts", "Maths", "t1", "Test", 1, 100, "DRAFT", "d", "Dee", 99)),
                GetClassGradebookUseCaseTest::grade);
        assertThat(tied.students()).extracting(s -> s.name() + "#" + s.position())
                .containsExactly("Ada#1", "Ben#1", "Cal#3", "Dee#null");
        assertThat(tied.students().get(2).passed()).isFalse();
    }
}
