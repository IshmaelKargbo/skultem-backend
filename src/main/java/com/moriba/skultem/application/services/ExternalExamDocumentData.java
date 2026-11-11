package com.moriba.skultem.application.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.model.ExternalExamSitting;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.service.ExternalExamResults;

/**
 * A candidate's external exam result as the data an M-Compose result slip or statement of result fills in. The
 * names match the ready-made designs "exam-result-slip" and "exam-statement-of-result"; images are data: URIs
 * (a transparent pixel when missing), as the engine needs.
 */
public final class ExternalExamDocumentData {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private ExternalExamDocumentData() {
    }

    public static Map<String, Object> of(SchoolDTO school, ExternalExam exam, ExternalExamSitting sitting,
            String className, String academicYear, ExternalExamCandidate candidate, Student student,
            UnaryOperator<String> toImage) {
        var result = ExternalExamResults.of(exam, sitting.getSubjects(), candidate.getScores(), candidate.getGrades());
        Map<String, Object> d = new LinkedHashMap<>();

        d.put("organisation", text(school == null ? null : school.name()));
        d.put("school_motto", text(school == null ? null : school.motto()));
        d.put("school_address", school == null || school.address() == null ? "" : join(", ",
                school.address().street(), school.address().city(), school.address().district(),
                school.address().region()));
        d.put("logo", image(school == null ? null : school.logo(), toImage));
        d.put("principal", text(school == null ? null : school.principalName()));
        d.put("principal_signature", image(school == null ? null : school.principalSignature(), toImage));

        d.put("exam_code", text(exam.getCode()));
        d.put("exam_name", text(exam.getName()));
        d.put("exam_body", text(exam.getExamBody()));
        d.put("exam_year", text(academicYear));
        d.put("centre_number", text(sitting.getCentreNumber()));
        d.put("class", text(className));
        d.put("document_title", exam.getCode() + " Statement of Result");

        d.put("candidate", text(candidate.getStudentName()));
        d.put("index_number", text(candidate.getIndexNumber()));
        d.put("admission_number", text(candidate.getAdmissionNumber()));
        d.put("gender", student == null || student.getGender() == null ? ""
                : student.getGender().name().charAt(0) + student.getGender().name().substring(1).toLowerCase());
        d.put("date_of_birth", student == null || student.getDateOfBirth() == null ? ""
                : DATE.format(student.getDateOfBirth()));
        d.put("photo", image(student == null ? null : student.getPhoto(), toImage));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (var r : result.subjects()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subject", text(r.subject()));
            row.put("score", r.absent() ? "-" : ReportCardDocumentData.score(r.score()));
            row.put("grade", text(r.grade()));
            row.put("points", r.points() == null ? "" : String.valueOf(r.points()));
            row.put("remark", text(r.remark()));
            row.put("score_percent", r.absent() ? "-" : r.score() == null ? "" : ReportCardDocumentData.score(r.score()) + "%");
            rows.add(row);
        }
        d.put("results", rows);
        d.put("subjects_sat", String.valueOf(result.subjectsSat()));
        // Grades carry no scores, so there is no total score for them.
        d.put("total_score", exam.gradesEntered() ? "" : ReportCardDocumentData.score(result.totalScore()));
        d.put("aggregate", result.aggregate() == null ? "-" : String.valueOf(result.aggregate()));
        d.put("aggregate_label", result.aggregateLabel());
        d.put("grading_key", gradingKey(exam));
        d.put("date_issued", DATE.format(LocalDate.now(ZoneId.of("Africa/Freetown"))));
        d.put("confirmed_by", text(sitting.getConfirmedByName()));
        d.put("document_id", text(candidate.getId()));

        // The same facts under the names report card and certificate designs use, so a school can print exam
        // results with one of those too.
        d.put("school", d.get("organisation"));
        d.put("place", text(school == null || school.address() == null ? null : school.address().city()));
        d.put("student", d.get("candidate"));
        d.put("student_id", d.get("index_number"));
        d.put("term", d.get("exam_code"));
        d.put("period", join(" · ", exam.getCode(), academicYear));
        d.put("report_title", exam.getCode() + " results");
        d.put("total", d.get("total_score"));
        d.put("overall_grade", d.get("aggregate"));
        d.put("date", d.get("date_issued"));
        d.put("issue_date", d.get("date_issued"));
        List<Map<String, Object>> subjectRows = new ArrayList<>();
        for (var row : rows) {
            row.put("remarks", row.get("remark"));
            row.put("final_grade", row.get("grade"));
            row.put("comment", row.get("remark"));
            Map<String, Object> r = new LinkedHashMap<>(row);
            // Exams entered as grades have no score: their subjects table shows the grade.
            r.put("grade", "-".equals(row.get("score")) ? "-"
                    : String.valueOf(row.get("score")).isEmpty() ? row.get("grade") : row.get("score") + "%");
            subjectRows.add(r);
        }
        d.put("subjects", subjectRows);
        // A certificate: who, why, and who signs.
        d.put("recipient", d.get("candidate"));
        d.put("event", exam.getName() + " " + text(academicYear));
        d.put("issuer", d.get("organisation"));
        d.put("reason", "for sitting the " + exam.getName() + " (" + exam.getCode() + ") " + text(academicYear)
                + (result.aggregate() == null ? "" : ", with " + result.aggregateLabel().toLowerCase() + " "
                        + result.aggregate()) + ".");
        d.put("signatory1", d.get("principal"));
        d.put("role1", "Principal");
        d.put("signatory2", d.get("confirmed_by"));
        d.put("role2", text(sitting.getConfirmedByName()).isEmpty() ? "" : "Examinations Officer");
        d.put("title", "Statement of Result");
        d.put("signatory", d.get("principal"));
        d.put("signatory_title", "Principal");
        d.put("phone", text(school == null ? null : school.phone()));
        // The school's brand colours: a template that follows them prints in this school's colours.
        d.put("colors", BrandColors.of(school));
        return d;
    }

    /** Every field of(...) fills in - what a design is checked against (see DocumentDesignService). */
    public static final java.util.Set<String> FIELDS = java.util.Set.of("organisation", "school_motto",
            "school_address", "logo", "principal", "principal_signature", "exam_code", "exam_name", "exam_body",
            "exam_year", "centre_number", "class", "document_title", "candidate", "index_number", "admission_number",
            "gender", "date_of_birth", "photo", "results", "subjects_sat", "total_score", "aggregate",
            "aggregate_label", "grading_key", "date_issued", "confirmed_by", "document_id", "school", "place",
            "student", "student_id", "term", "period", "report_title", "total", "overall_grade", "date", "issue_date",
            "subjects", "recipient", "event", "issuer", "reason", "signatory1", "role1", "signatory2", "role2",
            "title", "signatory", "signatory_title", "phone", "colors");

    private static String gradingKey(ExternalExam exam) {
        List<String> parts = new ArrayList<>();
        for (var g : exam.getGradeScale()) {
            parts.add(g.grade() + " " + g.minScore() + "-" + g.maxScore());
        }
        return "Grading: " + String.join(" · ", parts);
    }

    private static String image(String url, UnaryOperator<String> toImage) {
        if (url == null || url.isBlank()) {
            return ReportCardDocumentData.NO_IMAGE;
        }
        if (url.startsWith("data:image/")) {
            return url;
        }
        String data = toImage.apply(url);
        return data == null || data.isBlank() ? ReportCardDocumentData.NO_IMAGE : data;
    }

    private static String join(String sep, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                kept.add(p.trim());
            }
        }
        return String.join(sep, kept);
    }

    private static String text(String v) {
        return Objects.toString(v, "").trim();
    }
}
