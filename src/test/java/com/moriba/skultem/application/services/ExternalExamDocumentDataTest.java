package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.model.ExternalExamSitting;
import com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject;

class ExternalExamDocumentDataTest {

    private static Map<String, Object> data() {
        var d = ExternalExamDefaults.all().get(1); // BECE
        var exam = ExternalExam.create("e", "s", d.code(), d.name(), d.examBody(), d.level(), d.scale(), d.mode(),
                d.bestOf(), true);
        var sitting = ExternalExamSitting.create("x", "s", "e", "c", "y", "1020304",
                List.of(new ExamSubject("m", "Mathematics"), new ExamSubject("b", "Biology")));
        sitting.confirm("Mrs. F. Kamara");
        var candidate = ExternalExamCandidate.create("c1", "s", "x", "st", "Aminata Bangura", "S-1");
        candidate.record("1020304-001", Map.of("m", 78.0), Set.of("m", "b"));
        return ExternalExamDocumentData.of(null, exam, sitting, "JSS 3", "2026/2027", candidate, null, url -> null);
    }

    @Test
    void theFieldListIsExactlyWhatIsSent() {
        assertThat(data().keySet()).containsExactlyInAnyOrderElementsOf(ExternalExamDocumentData.FIELDS);
    }

    @Test
    void fillsReportCardAndCertificateNamesToo() {
        var d = data();
        assertThat(d.get("student")).isEqualTo("Aminata Bangura");
        assertThat(d.get("student_id")).isEqualTo("1020304-001");
        assertThat(d.get("recipient")).isEqualTo("Aminata Bangura");
        assertThat((String) d.get("reason")).contains("Basic Education Certificate Examination (BECE) 2026/2027")
                .contains("aggregate (best 6) 1");
        assertThat(d.get("signatory2")).isEqualTo("Mrs. F. Kamara");
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) d.get("results");
        assertThat(rows.get(0)).containsEntry("remarks", "Excellent").containsEntry("final_grade", "1");
        @SuppressWarnings("unchecked")
        var subjects = (List<Map<String, Object>>) d.get("subjects");
        assertThat(subjects.get(0)).containsEntry("grade", "78%");
        assertThat(subjects.get(1)).containsEntry("grade", "-");
    }

    @Test
    void anExamEnteredAsGradesPrintsTheGradesWithoutScores() {
        var d = ExternalExamDefaults.all().get(1); // BECE
        var exam = ExternalExam.create("e", "s", d.code(), d.name(), d.examBody(), d.level(), d.scale(), d.mode(),
                d.bestOf(), true).withEntryMode(ExternalExam.EntryMode.GRADES);
        var sitting = ExternalExamSitting.create("x", "s", "e", "c", "y", null,
                List.of(new ExamSubject("m", "Mathematics"), new ExamSubject("b", "Biology")));
        var candidate = ExternalExamCandidate.create("c1", "s", "x", "st", "Aminata Bangura", "S-1");
        candidate.recordGrades("1020304-001", Map.of("m", "2"), Set.of("m", "b"),
                g -> exam.band(g) == null ? null : exam.band(g).grade());

        var data = ExternalExamDocumentData.of(null, exam, sitting, "JSS 3", "2026/2027", candidate, null, u -> null);
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) data.get("results");
        assertThat(rows.get(0)).containsEntry("grade", "2").containsEntry("points", "2")
                .containsEntry("remark", "Very good").containsEntry("score", "");
        // Biology has no grade: the candidate doesn't take it, so it isn't on their result.
        assertThat(rows).hasSize(1);
        assertThat(data.get("aggregate")).isEqualTo("2");
        assertThat(data.get("total_score")).isEqualTo("");
        @SuppressWarnings("unchecked")
        var subjects = (List<Map<String, Object>>) data.get("subjects");
        assertThat(subjects.get(0)).containsEntry("grade", "2");
    }
}
