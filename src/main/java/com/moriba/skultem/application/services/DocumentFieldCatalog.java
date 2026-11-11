package com.moriba.skultem.application.services;

import java.util.List;
import java.util.Map;

import com.moriba.skultem.domain.vo.DocumentKind;

/**
 * The data Skultem has for each kind of document, as a school sees it when linking a design's placeholders: a plain
 * label, a group, an example and its type (text, picture, or a list with row fields). Keys are the ones
 * ReportCardDocumentData / ExternalExamDocumentData send; ALIASES are the other names the same data is also sent
 * under, so a placeholder with one of them links itself.
 */
public final class DocumentFieldCatalog {

    public record Source(String key, String label, String group, String type, String example, List<Source> columns) {
        static Source text(String key, String label, String group, String example) {
            return new Source(key, label, group, "TEXT", example, List.of());
        }

        static Source image(String key, String label, String group) {
            return new Source(key, label, group, "IMAGE", "(picture)", List.of());
        }

        static Source list(String key, String label, String group, List<Source> columns) {
            return new Source(key, label, group, "LIST", columns.size() + " columns per row", columns);
        }
    }

    private DocumentFieldCatalog() {
    }

    private static final String SCHOOL = "School", CARD = "Report card", STUDENT = "Student", RESULTS = "Results",
            COMMENTS = "Comments and signatures", EXAM = "Exam", CANDIDATE = "Candidate", CERT = "Certificate wording";

    private static final List<Source> REPORT_CARD = List.of(
            Source.text("organisation", "School name", SCHOOL, "Moriba International School"),
            Source.text("school_motto", "School motto", SCHOOL, "Learn and serve"),
            Source.text("school_address", "School address", SCHOOL, "12 Main Road, Freetown"),
            Source.text("school_phone", "School phone", SCHOOL, "+232 76 000000"),
            Source.text("place", "School town", SCHOOL, "Freetown"),
            Source.image("logo", "School logo", SCHOOL),
            Source.text("header_color", "Header colour", SCHOOL, "#1f3a8a"),
            Source.text("footer_note", "Footer note", SCHOOL, "Keep learning"),
            Source.text("report_title", "Report title", CARD, "Student progress report"),
            Source.text("term", "Term", CARD, "Term 2"),
            Source.text("academic_year", "Academic year", CARD, "2026/2027"),
            Source.text("period", "Term and year", CARD, "Term 2 · 2026/2027"),
            Source.text("scope", "Assessments covered", CARD, "Test 1 + Exam"),
            Source.text("next_term", "Next term begins", CARD, ""),
            Source.text("date", "Date issued", CARD, "11 November 2026"),
            Source.text("review_status", "Review status", CARD, "APPROVED"),
            Source.text("document_id", "Report card id", CARD, "rc-1"),
            Source.text("student", "Student name", STUDENT, "Jordan Rivera"),
            Source.text("student_id", "Admission number", STUDENT, "S-2041"),
            Source.text("class", "Class", STUDENT, "JSS 2 Blue"),
            Source.text("class_size", "Class size", STUDENT, "32"),
            Source.image("photo", "Student photo", STUDENT),
            Source.list("results", "Subject results", RESULTS, List.of(
                    Source.text("subject", "Subject", RESULTS, "Mathematics"),
                    Source.text("teacher", "Subject teacher", RESULTS, "Mr. Kamara"),
                    Source.text("score", "Score", RESULTS, "88"),
                    Source.text("score_percent", "Score with %", RESULTS, "88%"),
                    Source.text("weight", "Weight", RESULTS, "2"),
                    Source.text("weighted_score", "Weighted score", RESULTS, "176"),
                    Source.text("grade", "Grade", RESULTS, "A"),
                    Source.text("remarks", "Remark", RESULTS, "Excellent"),
                    Source.text("breakdown", "Assessment breakdown", RESULTS, "Test 1: 18 · Exam: 70"))),
            Source.text("subject_count", "Number of subjects", RESULTS, "10"),
            Source.text("total", "Total score", RESULTS, "799"),
            Source.text("average", "Average", RESULTS, "79.9"),
            Source.text("overall_grade", "Overall grade", RESULTS, "B"),
            Source.text("passed", "Passed or not", RESULTS, "Passed"),
            Source.text("position", "Position in class", RESULTS, "2nd"),
            Source.text("attendance", "Attendance", RESULTS, "96%"),
            Source.text("grading_key", "Grading key", RESULTS, "Grading: A 80-100 · B 70-79"),
            Source.text("teacher_comment", "Class teacher's comment", COMMENTS, "Works with focus."),
            Source.text("principal_comment", "Principal's remark (remark scale)", COMMENTS, "Very good performance."),
            Source.text("teacher", "Class teacher's name", COMMENTS, "Ms. A. Conteh"),
            Source.text("principal", "Principal's name", COMMENTS, "Dr. K. Bangura"),
            Source.image("teacher_signature", "Class teacher's signature", COMMENTS),
            Source.image("principal_signature", "Principal's signature", COMMENTS));

    private static final List<Source> EXAM_RESULT = List.of(
            Source.text("organisation", "School name", SCHOOL, "Moriba International School"),
            Source.text("school_motto", "School motto", SCHOOL, "Learn and serve"),
            Source.text("school_address", "School address", SCHOOL, "12 Main Road, Freetown"),
            Source.text("phone", "School phone", SCHOOL, "+232 76 000000"),
            Source.text("place", "School town", SCHOOL, "Freetown"),
            Source.image("logo", "School logo", SCHOOL),
            Source.text("principal", "Principal's name", SCHOOL, "Dr. K. Bangura"),
            Source.image("principal_signature", "Principal's signature", SCHOOL),
            Source.text("exam_code", "Exam code", EXAM, "BECE"),
            Source.text("exam_name", "Exam name", EXAM, "Basic Education Certificate Examination"),
            Source.text("exam_body", "Exam body", EXAM, "WAEC"),
            Source.text("exam_year", "Exam year", EXAM, "2026/2027"),
            Source.text("period", "Exam and year", EXAM, "BECE · 2026/2027"),
            Source.text("centre_number", "Centre number", EXAM, "1020304"),
            Source.text("class", "Class", EXAM, "JSS 3 Blue"),
            Source.text("document_title", "Document title", EXAM, "BECE Statement of Result"),
            Source.text("report_title", "Results title", EXAM, "BECE results"),
            Source.text("date_issued", "Date issued", EXAM, "11 November 2026"),
            Source.text("confirmed_by", "Confirmed by", EXAM, "Mrs. F. Kamara"),
            Source.text("document_id", "Result id", EXAM, "c-1"),
            Source.text("candidate", "Candidate name", CANDIDATE, "Aminata Bangura"),
            Source.text("index_number", "Index number", CANDIDATE, "1020304-001"),
            Source.text("admission_number", "Admission number", CANDIDATE, "S-2041"),
            Source.text("gender", "Gender", CANDIDATE, "Female"),
            Source.text("date_of_birth", "Date of birth", CANDIDATE, "4 March 2012"),
            Source.image("photo", "Candidate photo", CANDIDATE),
            Source.list("results", "Subject results", RESULTS, List.of(
                    Source.text("subject", "Subject", RESULTS, "Mathematics"),
                    Source.text("score", "Score", RESULTS, "71"),
                    Source.text("score_percent", "Score with %", RESULTS, "71%"),
                    Source.text("grade", "Grade", RESULTS, "2"),
                    Source.text("points", "Points", RESULTS, "2"),
                    Source.text("remark", "Remark", RESULTS, "Very good"))),
            Source.text("subjects_sat", "Subjects sat", RESULTS, "7"),
            Source.text("total_score", "Total score", RESULTS, "468"),
            Source.text("aggregate", "Aggregate", RESULTS, "12"),
            Source.text("aggregate_label", "Aggregate label", RESULTS, "Aggregate (best 6)"),
            Source.text("grading_key", "Grading key", RESULTS, "Grading: 1 75-100 · 2 65-74"),
            Source.text("title", "Title (Statement of Result)", CERT, "Statement of Result"),
            Source.text("event", "Exam name and year", CERT, "Basic Education Certificate Examination 2026/2027"),
            Source.text("reason", "Certificate wording", CERT, "for sitting the ... with aggregate 12."),
            Source.text("role1", "First signatory's title", CERT, "Principal"),
            Source.text("role2", "Second signatory's title", CERT, "Examinations Officer"),
            Source.text("signatory_title", "Signatory's title", CERT, "Principal"));

    private static final Map<String, String> REPORT_CARD_ALIASES = Map.ofEntries(Map.entry("school", "organisation"),
            Map.entry("phone", "school_phone"), Map.entry("issue_date", "date"), Map.entry("advisor", "teacher"),
            Map.entry("sessions", "attendance"), Map.entry("subjects", "results"), Map.entry("address", "school_address"),
            Map.entry("motto", "school_motto"), Map.entry("admission_number", "student_id"));

    private static final Map<String, String> EXAM_ALIASES = Map.ofEntries(Map.entry("school", "organisation"),
            Map.entry("student", "candidate"), Map.entry("student_id", "index_number"), Map.entry("term", "exam_code"),
            Map.entry("total", "total_score"), Map.entry("overall_grade", "aggregate"), Map.entry("date", "date_issued"),
            Map.entry("issue_date", "date_issued"), Map.entry("subjects", "results"), Map.entry("recipient", "candidate"),
            Map.entry("issuer", "organisation"), Map.entry("signatory1", "principal"), Map.entry("signatory", "principal"),
            Map.entry("signatory2", "confirmed_by"), Map.entry("address", "school_address"),
            Map.entry("centre", "centre_number"), Map.entry("motto", "school_motto"), Map.entry("sex", "gender"),
            Map.entry("exams_officer", "confirmed_by"), Map.entry("dob", "date_of_birth"),
            Map.entry("index", "index_number"));

    public static List<Source> sources(DocumentKind kind) {
        return kind == DocumentKind.REPORT_CARD ? REPORT_CARD : EXAM_RESULT;
    }

    /** The data a placeholder of this name gets without being linked (same name, or another name for it). */
    public static Source automatic(DocumentKind kind, String placeholder) {
        var aliases = kind == DocumentKind.REPORT_CARD ? REPORT_CARD_ALIASES : EXAM_ALIASES;
        String key = aliases.getOrDefault(placeholder, placeholder);
        return find(kind, key);
    }

    public static Source find(DocumentKind kind, String key) {
        return sources(kind).stream().filter(s -> s.key().equals(key)).findFirst().orElse(null);
    }
}
