package com.moriba.skultem.application.dto;

import java.util.List;

// Whether each class's students have the assessments its template should give them (this academic year).
// status: OK, NO_STUDENTS, NO_TEMPLATE (students but no template), EMPTY_TEMPLATE (template has no tests),
// NO_SUBJECTS (no subjects assigned to the class), LATER_START (the template starts grading after the current term, so
// there's nothing to enter yet - set on the template), MISSING (some students have no assessments at all) or INCOMPLETE (some have fewer than their classmates -
// e.g. a subject was added or got its teacher later).
public record AssessmentCoverageDTO(String academicYearName, Integer activeTermNumber, List<Row> rows) {

    public record Row(String classId, String className, String level, String templateId, String templateName,
            int templateTests, int startsInTerm, int subjects, int students, int withoutAssessments, int incomplete,
            List<String> subjectsWithoutTeacher, String status) {
    }
}
