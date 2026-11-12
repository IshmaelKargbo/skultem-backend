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
        return of(card, null, toImage);
    }

    /** As above, with the student's own record for what the card doesn't hold (gender, date of birth, age). */
    public static Map<String, Object> of(ReportCardDTO card, com.moriba.skultem.domain.model.Student student,
            UnaryOperator<String> toImage) {
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
        // Everything is sent; the design (and its groups, on Data Links) decides what shows.
        d.put("photo", image(card.photo(), toImage));

        // Results
        List<ReportCardSubjectDTO> subjects = card.subjects() == null ? List.of() : card.subjects();
        List<Map<String, Object>> results = new ArrayList<>();
        // The card's own columns, as Skultem's report card shows them: each assessment (by its order in the
        // template), or each term on a whole-year card. Each row carries its mark per column as col_1, col_2, ...
        List<String> columns = resultColumns(subjects, yearCard);
        double total = 0;
        for (var s : subjects) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                row.put("col_" + (i + 1), columnValue(s, columns.get(i), yearCard));
            }
            row.put("subject", text(s.subject()));
            row.put("teacher", text(s.teacher()));
            row.put("score", score(s.score()));
            row.put("weight", s.weight() == null ? "" : s.weight());
            row.put("weighted_score", score(s.weightScore()));
            row.put("grade", text(s.grade()));
            row.put("breakdown", breakdown(s));
            row.put("score_percent", s.score() == null ? "" : score(s.score()) + "%");
            // The name M-Compose's report card designs use for a row's total. Rows carry no remark: the card's
            // remarks are its comments (teacher's, principal's, overall).
            row.put("total", row.get("score"));
            results.add(row);
            if (s.score() != null) {
                total += s.score();
            }
        }
        d.put("results", results);
        d.put("result_columns", columns);
        // The table's columns, for a design whose table takes them from the data (columns_from: "{{columns}}"):
        // Subject, each assessment (with its weight) or term, Total, Grade.
        d.put("columns", tableColumns(subjects, columns, yearCard));
        d.put("subject_count", subjects.size());
        d.put("total", score(total));
        d.put("average", score(card.average()));
        d.put("overall_grade", text(card.overallGrade()));
        d.put("passed", card.passed() ? "Passed" : "Not passed");
        d.put("position", card.position() > 0 ? ordinal(card.position()) : "");
        d.put("attendance", card.attendancePercentage() != null
                ? score(card.attendancePercentage()) + "%" : "");
        d.put("grading_key", gradingKey(school == null ? null : school.gradingScale()));

        // Comments and signatures
        d.put("teacher_comment", text(card.remark()));
        d.put("principal_comment", text(card.gradeRemark()));
        d.put("teacher", text(card.classTeacherName()));
        d.put("principal", text(school == null ? null : school.principalName()));
        d.put("teacher_signature", image(card.classTeacherSignature(), toImage));
        d.put("principal_signature", image(school != null ? school.principalSignature() : null,
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
            r.put("final_grade", row.get("grade"));
            subjectRows.add(r);
        }
        d.put("subjects", subjectRows);


        // From the student's record.
        var gender = student == null ? null : student.getGender();
        d.put("gender", gender == null ? "" : gender.name().charAt(0) + gender.name().substring(1).toLowerCase());
        var dob = student == null ? null : student.getDateOfBirth();
        d.put("date_of_birth", dob == null ? "" : DATE.format(dob));
        d.put("age", dob == null ? ""
                : String.valueOf(java.time.Period.between(dob, java.time.LocalDate.now(java.time.ZoneId.of("Africa/Freetown"))).getYears()));
        // The card's overall remark, from the school's remark scale for the average.
        d.put("remark", d.get("principal_comment"));
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
            "advisor", "sessions", "subjects", "colors", "result_columns", "columns", "gender", "date_of_birth", "age",
            "remark");

    private static final java.time.format.DateTimeFormatter DATE =
            java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH);

    /** The column headings of the card's results: assessments in their template order, or the terms of the year. */
    static List<String> resultColumns(List<ReportCardSubjectDTO> subjects, boolean yearCard) {
        List<String> names = new ArrayList<>();
        if (yearCard) {
            for (var s : subjects) {
                if (s.termScores() == null) {
                    continue;
                }
                for (var t : s.termScores()) {
                    if (t.term() != null && !names.contains(t.term())) {
                        names.add(t.term());
                    }
                }
            }
            return names;
        }
        Map<String, Integer> level = new LinkedHashMap<>();
        for (var s : subjects) {
            if (s.assessments() == null) {
                continue;
            }
            for (var a : s.assessments()) {
                if (a.name() != null) {
                    level.putIfAbsent(a.name(), a.level() == null ? Integer.MAX_VALUE : a.level());
                }
            }
        }
        level.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> names.add(e.getKey()));
        return names;
    }

    /** The column list M-Compose builds the results table from (each a field, label, width and alignment). */
    public static List<Map<String, Object>> tableColumns(List<ReportCardSubjectDTO> subjects, List<String> marks,
            boolean yearCard) {
        Map<String, Integer> weight = new LinkedHashMap<>();
        for (var s : subjects) {
            if (s.assessments() != null) {
                for (var a : s.assessments()) {
                    if (a.name() != null && a.weight() != null) {
                        weight.putIfAbsent(a.name(), a.weight());
                    }
                }
            }
        }
        int markWidth = marks.size() <= 3 ? 52 : 46;
        List<Map<String, Object>> cols = new ArrayList<>();
        cols.add(column("subject", "Subject", null, "left"));
        for (int i = 0; i < marks.size(); i++) {
            String name = marks.get(i);
            String label = !yearCard && weight.containsKey(name) ? name + " (" + weight.get(name) + ")" : name;
            // Wide enough for the heading on one line ("Test 1 (50)" does not wrap); the subject takes the rest.
            cols.add(column("col_" + (i + 1), label, Math.max(markWidth, headingWidth(label)), "center"));
        }
        cols.add(column("total", yearCard ? "Final" : "Total", 50, "center"));
        cols.add(column("grade", "Grade", 44, "center"));
        // No per-subject remark column: the card's remarks are its comments (teacher's, principal's, overall).
        return cols;
    }

    // The width a column needs for its heading on one line: the heading in bold 10pt (the size table headings print
    // at), plus the cell's padding. Measured with the sans-serif font Helvetica is drawn like.
    private static final java.awt.Font HEADING_FONT = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10);
    private static final java.awt.font.FontRenderContext CONTEXT =
            new java.awt.font.FontRenderContext(null, true, true);

    static int headingWidth(String label) {
        double text = HEADING_FONT.getStringBounds(label == null ? "" : label, CONTEXT).getWidth();
        return (int) Math.ceil(text) + 14;
    }

    private static Map<String, Object> column(String field, String label, Integer width, String alignment) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("field", field);
        c.put("label", label);
        if (width != null) {
            c.put("width", width);
        }
        c.put("alignment", alignment);
        return c;
    }

    private static String columnValue(ReportCardSubjectDTO s, String column, boolean yearCard) {
        if (yearCard) {
            return s.termScores() == null ? "-" : s.termScores().stream().filter(t -> column.equals(t.term()))
                    .findFirst().map(t -> t.score() == null ? "-" : score(t.score())).orElse("-");
        }
        return s.assessments() == null ? "-" : s.assessments().stream().filter(a -> column.equals(a.name()))
                .findFirst().map(a -> a.score() == null ? "-" : String.valueOf(a.score())).orElse("-");
    }

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
