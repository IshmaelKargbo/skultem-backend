package com.moriba.skultem.infrastructure.compose;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.moriba.skultem.application.dto.ReportCardAssessmentScoreDTO;
import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.application.services.ReportCardDocumentData;

/**
 * Against a real M-Compose. Skipped unless MCOMPOSE_TEST_URL and MCOMPOSE_TEST_API_KEY are set, e.g.
 * MCOMPOSE_TEST_URL=http://127.0.0.1:8000 MCOMPOSE_TEST_API_KEY=mck_... mvn test -Dtest=MComposeLiveTest
 * Set MCOMPOSE_TEST_OUT to a folder to keep the PDFs and look at them.
 */
@EnabledIfEnvironmentVariable(named = "MCOMPOSE_TEST_URL", matches = ".+")
class MComposeLiveTest {

    private final MComposeClient client = new MComposeClient(System.getenv("MCOMPOSE_TEST_URL"),
            System.getenv("MCOMPOSE_TEST_API_KEY"), 60);

    private static ReportCardDTO card(String name, int position) {
        var subjects = List.of(
                new ReportCardSubjectDTO("Mathematics", "Mr. Kamara", 88.0, null, null, "A",
                        List.of(new ReportCardAssessmentScoreDTO("Test 1", 18, 20, 1),
                                new ReportCardAssessmentScoreDTO("Exam", 70, 80, 2)), null),
                new ReportCardSubjectDTO("English language", "Mrs. Sesay", 71.5, null, null, "B", List.of(), null),
                new ReportCardSubjectDTO("Integrated science", "Mr. Koroma", 64.0, null, null, "C", List.of(), null));
        return new ReportCardDTO("rc-" + position, "st-" + position, name, "S-20" + position, null, "c1",
                "JSS 2 Blue", 32, "t1", "Term 1", "2026/2027", 74.5, position, "B", true, 96.0,
                "Works with focus and curiosity.", subjects, null, 0, null, null, null, null,
                "Very good performance. Keep it up.", "APPROVED", null, null, null, "Ms. A. Conteh", null);
    }

    @Test
    void rendersAReportCardAndAClassBatchWithTheReadyMadeDesign() throws Exception {
        byte[] one = client.render("sample:report-card-classic",
                ReportCardDocumentData.of(card("Jordan Rivera", 2), url -> null));
        byte[] batch = client.renderBatch("sample:report-card-classic", java.util.Map.of(), List.of(
                ReportCardDocumentData.of(card("Jordan Rivera", 2), url -> null),
                ReportCardDocumentData.of(card("Aminata Bangura", 1), url -> null)));

        assertThat(new String(Arrays.copyOf(one, 5), StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(Arrays.copyOf(batch, 5), StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(batch.length).isGreaterThan(one.length);

        String out = System.getenv("MCOMPOSE_TEST_OUT");
        if (out != null && !out.isBlank()) {
            Files.write(Path.of(out, "report-card.pdf"), one);
            Files.write(Path.of(out, "report-cards-class.pdf"), batch);
        }
    }

    @Test
    void rendersExternalExamSlipsAndStatements() throws Exception {
        var d = com.moriba.skultem.application.services.ExternalExamDefaults.all().get(2); // WASSCE
        var exam = com.moriba.skultem.domain.model.ExternalExam.create("e1", "s1", d.code(), d.name(), d.examBody(),
                d.level(), d.scale(), d.mode(), d.bestOf(), true);
        var subjects = List.of(
                new com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject("m", "Mathematics"),
                new com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject("e", "English Language"),
                new com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject("b", "Biology"));
        var sitting = com.moriba.skultem.domain.model.ExternalExamSitting.create("x1", "s1", "e1", "c1", "y1",
                "1020304", subjects);
        sitting.confirm("Mrs. F. Kamara");
        var candidate = com.moriba.skultem.domain.model.ExternalExamCandidate.create("c-1", "s1", "x1", "st1",
                "Jordan Rivera", "S-2041");
        candidate.record("1020304-007", java.util.Map.of("m", 77.0, "e", 63.0), java.util.Set.of("m", "e", "b"));
        var data = com.moriba.skultem.application.services.ExternalExamDocumentData.of(null, exam, sitting,
                "SSS 3 Science", "2026/2027", candidate, null, url -> null);

        byte[] slip = client.render("sample:exam-result-slip", data);
        byte[] statement = client.renderBatch("sample:exam-statement-of-result", java.util.Map.of(),
                List.of(data, data));
        assertThat(new String(Arrays.copyOf(slip, 5), StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(Arrays.copyOf(statement, 5), StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        String out = System.getenv("MCOMPOSE_TEST_OUT");
        if (out != null && !out.isBlank()) {
            Files.write(Path.of(out, "exam-slip.pdf"), slip);
            Files.write(Path.of(out, "exam-statements.pdf"), statement);
        }
    }
}
