package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.dto.ReportCardAssessmentScoreDTO;
import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.application.dto.ReportCardTermScoreDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.GradeBand;
import com.moriba.skultem.domain.vo.RemarkBand;

class ReportCardDocumentDataTest {

    private static final String LOGO_URL = "https://bucket.example/logo.png";
    private static final String LOGO_DATA = "data:image/png;base64,TE9HTw==";

    private static SchoolDTO school() {
        return new SchoolDTO("s1", "Moriba International School", "moriba",
                new Address("Western", "Freetown", null, "Freetown", "12 Main Road"), null, null,
                List.of(new GradeBand(50, 69, "B"), new GradeBand(70, 100, "A"), new GradeBand(0, 49, "F")),
                LOGO_URL, "Learn and serve", "Dr. K. Bangura", null, "#123456", null, null, null, false, null,
                null, null, "+232 76 000000", 0, 0, 0, null, false, 1);
    }

    private static ReportCardSettingDTO settings(boolean showPosition, Boolean showPhoto) {
        return new ReportCardSettingDTO(null, null, "Keep learning", true, true, showPosition, true, true, true, true,
                List.of(new RemarkBand(0, 69, "Keep working"), new RemarkBand(70, 100, "Excellent")), showPhoto);
    }

    private static ReportCardDTO card(List<ReportCardSubjectDTO> subjects, ReportCardSettingDTO settings) {
        return new ReportCardDTO("rc-1", "st-1", "Jordan Rivera", "S-2041", "https://bucket.example/photo.png",
                "c1", "Grade 8 Blue", 32, "t2", "Term 2", "2026/2027", 79.94, 2, "A", true, 96.0,
                "Works with focus.", subjects, null, 0, school(), settings, null, null, "Very good performance.",
                "APPROVED", null, null, null, "Ms. A. Conteh", null);
    }

    private static final List<ReportCardSubjectDTO> TERM_SUBJECTS = List.of(
            new ReportCardSubjectDTO("Mathematics", "Mr. Kamara", 88.0, null, null, "A",
                    List.of(new ReportCardAssessmentScoreDTO("Test 1", 18, 20, 1),
                            new ReportCardAssessmentScoreDTO("Exam", 70, 80, 2)),
                    null),
            new ReportCardSubjectDTO("English", "Mrs. Sesay", 61.5, null, null, "B", List.of(), null));

    private static Map<String, Object> map(ReportCardDTO card) {
        return ReportCardDocumentData.of(card, url -> url.equals(LOGO_URL) ? LOGO_DATA : null);
    }

    @Test
    void fillsEveryNameTheReadyMadeReportCardsUse() {
        var d = map(card(TERM_SUBJECTS, settings(true, null)));

        // The bindings of the engine's report-card-classic design.
        assertThat(d).containsKeys("attendance", "average", "class", "class_size", "grading_key", "next_term",
                "organisation", "overall_grade", "period", "position", "principal", "principal_comment",
                "report_title", "results", "student", "student_id", "teacher", "teacher_comment", "term", "total");
        assertThat(d.get("organisation")).isEqualTo("Moriba International School");
        assertThat(d.get("period")).isEqualTo("Term 2 · 2026/2027");
        assertThat(d.get("student")).isEqualTo("Jordan Rivera");
        assertThat(d.get("average")).isEqualTo("79.9");
        assertThat(d.get("total")).isEqualTo("149.5");
        assertThat(d.get("position")).isEqualTo("2nd");
        assertThat(d.get("attendance")).isEqualTo("96%");
        assertThat(d.get("grading_key")).isEqualTo("Grading: A 70-100 · B 50-69 · F 0-49");
        assertThat(d.get("teacher")).isEqualTo("Ms. A. Conteh");
        assertThat(d.get("principal")).isEqualTo("Dr. K. Bangura");
        assertThat(d.get("principal_comment")).isEqualTo("Very good performance.");
        assertThat(d.get("school_address")).isEqualTo("12 Main Road, Freetown, Freetown, Western");
    }

    @Test
    void oneResultRowPerSubjectWithItsBreakdownAndRemark() {
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) map(card(TERM_SUBJECTS, settings(true, null))).get("results");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("subject", "Mathematics").containsEntry("score", "88")
                .containsEntry("grade", "A").containsEntry("remarks", "Excellent")
                .containsEntry("teacher", "Mr. Kamara").containsEntry("breakdown", "Test 1: 18 · Exam: 70");
        assertThat(rows.get(1)).containsEntry("score", "61.5").containsEntry("remarks", "Keep working");
    }

    @Test
    void aWholeYearCardShowsEachTermsScore() {
        var subjects = List.of(new ReportCardSubjectDTO("Science", null, 75.0, null, null, "A", null,
                List.of(new ReportCardTermScoreDTO("Term 1", 70.0), new ReportCardTermScoreDTO("Term 2", 80.0))));
        var d = map(card(subjects, settings(true, null)));

        assertThat(d.get("report_title")).isEqualTo("End of year report");
        assertThat(d.get("period")).isEqualTo("2026/2027");
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) d.get("results");
        assertThat(rows.get(0).get("breakdown")).isEqualTo("Term 1: 70 · Term 2: 80");
    }

    @Test
    void imagesAreDataUrisAndAMissingOneIsATransparentPixel() {
        var d = map(card(TERM_SUBJECTS, settings(true, null)));

        assertThat(d.get("logo")).isEqualTo(LOGO_DATA);
        // The photo could not be loaded, and the principal has no signature: the engine still gets an image.
        assertThat(d.get("photo")).isEqualTo(ReportCardDocumentData.NO_IMAGE);
        assertThat(d.get("principal_signature")).isEqualTo(ReportCardDocumentData.NO_IMAGE);
    }

    @Test
    void respectsTheSchoolsReportCardSettings() {
        var loaded = new java.util.ArrayList<String>();
        var d = ReportCardDocumentData.of(card(TERM_SUBJECTS, settings(false, false)), url -> {
            loaded.add(url);
            return LOGO_DATA;
        });

        assertThat(d.get("position")).isEqualTo("");
        assertThat(d.get("photo")).isEqualTo(ReportCardDocumentData.NO_IMAGE);
        // A photo the school does not print is not even downloaded.
        assertThat(loaded).containsExactly(LOGO_URL);
    }

    @Test
    void theFieldListIsExactlyWhatIsSent() {
        assertThat(map(card(TERM_SUBJECTS, settings(true, null))).keySet())
                .containsExactlyInAnyOrderElementsOf(ReportCardDocumentData.FIELDS);
    }

    @Test
    void fillsTheNavySidebarDesignsNamesToo() {
        var d = map(card(TERM_SUBJECTS, settings(true, null)));
        assertThat(d.get("school")).isEqualTo("Moriba International School");
        assertThat(d.get("advisor")).isEqualTo("Ms. A. Conteh");
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) d.get("subjects");
        assertThat(rows.get(0)).containsEntry("subject", "Mathematics").containsEntry("grade", "88%")
                .containsEntry("final_grade", "A");
    }

    @Test
    void sendsTheSchoolsBrandColoursForTheTemplateToFollow() {
        var d = map(card(TERM_SUBJECTS, settings(true, null)));
        // school() has primary #123456 and no secondary colour.
        assertThat(d.get("colors")).isEqualTo(Map.of("primary", "#123456"));
        assertThat(BrandColors.of("#F66102", "#820013")).containsEntry("primary", "#f66102")
                .containsEntry("secondary", "#820013").containsEntry("accent", "#820013");
        assertThat(BrandColors.of("orange", "")).isEmpty();
    }

    @Test
    void anAssessmentBasedCardSaysWhichAssessmentsItCovers() {
        var c = card(TERM_SUBJECTS, settings(true, null));
        var firstAssessment = new ReportCardDTO(c.id(), c.studentId(), c.studentName(), c.admissionNumber(), c.photo(),
                c.classId(), c.className(), c.classSize(), c.termId(), c.termName(), c.academicYearName(), c.average(),
                c.position(), c.overallGrade(), c.passed(), c.attendancePercentage(), c.remark(), c.subjects(),
                c.generatedAt(), c.downloadCount(), c.school(), c.settings(), c.level(), "First Assessment",
                c.gradeRemark(), c.reviewStatus(), c.reviewNote(), c.reviewedByName(), c.reviewedAt(),
                c.classTeacherName(), c.classTeacherSignature());

        var d = map(firstAssessment);
        assertThat(d.get("term")).isEqualTo("Term 2 (First Assessment)");
        assertThat(d.get("period")).isEqualTo("Term 2 (First Assessment) · 2026/2027");
        // A whole-term card stays as it was.
        assertThat(map(c).get("term")).isEqualTo("Term 2");
    }

    @Test
    void scoresAndPlaces() {
        assertThat(ReportCardDocumentData.score(88.0)).isEqualTo("88");
        assertThat(ReportCardDocumentData.score(79.95)).isEqualTo("80");
        assertThat(ReportCardDocumentData.score(null)).isEqualTo("");
        assertThat(ReportCardDocumentData.ordinal(1)).isEqualTo("1st");
        assertThat(ReportCardDocumentData.ordinal(12)).isEqualTo("12th");
        assertThat(ReportCardDocumentData.ordinal(23)).isEqualTo("23rd");
        assertThat(ReportCardDocumentData.ordinal(111)).isEqualTo("111th");
    }
}
