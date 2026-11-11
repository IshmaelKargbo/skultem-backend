package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.domain.vo.GradeBand;

/**
 * A report card as the data an M-Compose template fills in. The template binds these names ({{student}},
 * {{results}}, ...). The names used by the engine's ready-made report cards (report-card-classic,
 * report-card-modern) are all here, so those designs work as they are; the rest are extras a design can use.
 *
 * Images (logo, photo, signatures) must be data: URIs - the engine reads no URLs. toImage turns a stored file's URL
 * into one, or returns null; a missing image becomes a transparent pixel, because the engine refuses to render a
 * template whose image has no value.
 */
public final class ReportCardDocumentData {

    /** A 1x1 transparent PNG: what an image that a card does not have is drawn as. */
    public static final String NO_IMAGE = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNgAAIAAAUAAen63NgAAAAASUVORK5CYII=";

    private ReportCardDocumentData() {
    }

    public static Map<String, Object> of(ReportCardDTO card, UnaryOperator<String> toImage) {
        var school = card.school();
        var settings = card.settings();
        Map<String, Object> d = new LinkedHashMap<>();

        // The school
        d.put("organisation", text(school == null ? null : school.name()));
        d.put("school_motto", text(school == null ? null : school.motto()));
        d.put("school_address", school == null ? "" : address(school.address()));
        d.put("school_phone", text(school == null ? null : school.phone()));
        d.put("header_color", text(settings == null || settings.headerColor() == null
                ? (school == null ? null : school.primaryColor())
                : settings.headerColor()));
        d.put("footer_note", text(settings == null ? null : settings.footerNote()));
        String logo = school != null && notBlank(school.logo()) ? school.logo()
                : settings == null ? null : settings.logoUrl();
        d.put("logo", image(logo, toImage));

        // The card
        boolean yearCard = card.subjects() != null && card.subjects().stream()
                .anyMatch(s -> s.termScores() != null && !s.termScores().isEmpty());
        d.put("report_title", yearCard ? "End of year report" : "Student progress report");
        // An assessment-based card (generated from some of the term's assessments) says which: "Term 1 (First
        // Assessment)". A whole-term card is just "Term 1"; a whole-year card's scope is "All terms".
        boolean assessmentBased = !yearCard && card.scopeLabel() != null && !card.scopeLabel().isBlank();
        String term = assessmentBased ? text(card.termName()) + " (" + card.scopeLabel().trim() + ")"
                : text(card.termName());
        d.put("term", term);
        d.put("academic_year", text(card.academicYearName()));
        d.put("scope", text(card.scopeLabel()));
        d.put("period", yearCard ? join(" · ", card.academicYearName(), card.scopeLabel())
                : join(" · ", term, card.academicYearName()));
        d.put("next_term", "");

        // The student
        d.put("student", text(card.studentName()));
        d.put("student_id", text(card.admissionNumber()));
        d.put("class", text(card.className()));
        d.put("class_size", card.classSize());
        d.put("photo", image(settings == null || !Boolean.FALSE.equals(settings.showStudentPhoto())
                ? card.photo() : null, toImage));

        // Results
        List<ReportCardSubjectDTO> subjects = card.subjects() == null ? List.of() : card.subjects();
        var bands = settings == null ? null : settings.remarkScale();
        List<Map<String, Object>> results = new ArrayList<>();
        double total = 0;
        for (var s : subjects) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subject", text(s.subject()));
            row.put("teacher", text(s.teacher()));
            row.put("score", score(s.score()));
            row.put("weight", s.weight() == null ? "" : s.weight());
            row.put("weighted_score", score(s.weightScore()));
            row.put("grade", text(s.grade()));
            row.put("remarks", s.score() == null ? "" : text(RemarkScale.resolve(bands, s.score())));
            row.put("breakdown", breakdown(s));
            row.put("score_percent", s.score() == null ? "" : score(s.score()) + "%");
            results.add(row);
            if (s.score() != null) {
                total += s.score();
            }
        }
        d.put("results", results);
        d.put("subject_count", subjects.size());
        d.put("total", score(total));
        d.put("average", score(card.average()));
        d.put("overall_grade", text(card.overallGrade()));
        d.put("passed", card.passed() ? "Passed" : "Not passed");
        boolean showPosition = settings == null || settings.showPosition();
        d.put("position", showPosition && card.position() > 0 ? ordinal(card.position()) : "");
        boolean showAttendance = settings == null || settings.showAttendance();
        d.put("attendance", showAttendance && card.attendancePercentage() != null
                ? score(card.attendancePercentage()) + "%" : "");
        d.put("grading_key", gradingKey(school == null ? null : school.gradingScale()));

        // Comments and signatures
        boolean showRemarks = settings == null || settings.showRemarks();
        d.put("teacher_comment", showRemarks ? text(card.remark()) : "");
        d.put("principal_comment", showRemarks ? text(card.gradeRemark()) : "");
        d.put("teacher", text(card.classTeacherName()));
        d.put("principal", text(school == null ? null : school.principalName()));
        boolean teacherSigns = settings == null || settings.showTeacherSignature();
        boolean principalSigns = settings == null || settings.showPrincipalSignature();
        d.put("teacher_signature", image(teacherSigns ? card.classTeacherSignature() : null, toImage));
        d.put("principal_signature", image(principalSigns && school != null ? school.principalSignature() : null,
                toImage));

        d.put("review_status", text(card.reviewStatus()));
        d.put("document_id", text(card.id()));

        // The same facts under the other names designs use (the navy sidebar report card: school, subjects with
        // final_grade, advisor, sessions...), so more designs come out filled in.
        d.put("school", d.get("organisation"));
        d.put("place", text(school == null || school.address() == null ? null : school.address().city()));
        d.put("phone", d.get("school_phone"));
        String today = DATE.format(java.time.LocalDate.now(java.time.ZoneId.of("Africa/Freetown")));
        d.put("date", today);
        d.put("issue_date", today);
        d.put("advisor", d.get("teacher"));
        d.put("sessions", d.get("attendance"));
        List<Map<String, Object>> subjectRows = new ArrayList<>();
        for (var row : results) {
            Map<String, Object> r = new LinkedHashMap<>(row);
            r.put("grade", row.get("score").toString().isEmpty() ? "" : row.get("score") + "%");
            r.put("final_grade", row.get("grade"));
            r.put("comment", row.get("remarks"));
            subjectRows.add(r);
        }
        d.put("subjects", subjectRows);
        // The school's brand colours: a template that follows them prints in this school's colours.
        d.put("colors", BrandColors.of(school));
        return d;
    }

    /** Every field of(...) fills in - what a design is checked against (see DocumentDesignService). */
    public static final java.util.Set<String> FIELDS = java.util.Set.of("organisation", "school_motto",
            "school_address", "school_phone", "header_color", "footer_note", "logo", "report_title", "term",
            "academic_year", "scope", "period", "next_term", "student", "student_id", "class", "class_size", "photo",
            "results", "subject_count", "total", "average", "overall_grade", "passed", "position", "attendance",
            "grading_key", "teacher_comment", "principal_comment", "teacher", "principal", "teacher_signature",
            "principal_signature", "review_status", "document_id", "school", "place", "phone", "date", "issue_date",
            "advisor", "sessions", "subjects", "colors");

    private static final java.time.format.DateTimeFormatter DATE =
            java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH);

    // "Test 1: 18 · Test 2: 15 · Exam: 52", or each term's score on a whole-year card.
    private static String breakdown(ReportCardSubjectDTO s) {
        if (s.termScores() != null && !s.termScores().isEmpty()) {
            return s.termScores().stream()
                    .map(t -> text(t.term()) + ": " + (t.score() == null ? "-" : score(t.score())))
                    .collect(Collectors.joining(" · "));
        }
        if (s.assessments() == null) {
            return "";
        }
        return s.assessments().stream()
                .map(a -> text(a.name()) + ": " + (a.score() == null ? "-" : a.score()))
                .collect(Collectors.joining(" · "));
    }

    private static String gradingKey(List<GradeBand> scale) {
        if (scale == null || scale.isEmpty()) {
            return "";
        }
        return "Grading: " + scale.stream()
                .sorted((a, b) -> Integer.compare(b.minScore(), a.minScore()))
                .map(b -> b.grade() + " " + b.minScore() + "-" + b.maxScore())
                .collect(Collectors.joining(" · "));
    }

    private static String address(com.moriba.skultem.domain.vo.Address a) {
        if (a == null) {
            return "";
        }
        return join(", ", a.street(), a.city(), a.chiefdom(), a.district(), a.region());
    }

    private static String image(String url, UnaryOperator<String> toImage) {
        if (!notBlank(url)) {
            return NO_IMAGE;
        }
        if (url.startsWith("data:image/")) {
            return url;
        }
        String data = toImage.apply(url);
        return notBlank(data) ? data : NO_IMAGE;
    }

    // Whole numbers without ".0", others to one decimal place: 88, 79.5.
    static String score(Double value) {
        if (value == null) {
            return "";
        }
        var rounded = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.setScale(0).toPlainString() : rounded.toPlainString();
    }

    static String ordinal(int n) {
        int mod100 = n % 100;
        String suffix = mod100 >= 11 && mod100 <= 13 ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return n + suffix;
    }

    private static String join(String separator, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String p : parts) {
            if (notBlank(p)) {
                kept.add(p.trim());
            }
        }
        return String.join(separator, kept);
    }

    private static String text(String value) {
        return Objects.toString(value, "").trim();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
