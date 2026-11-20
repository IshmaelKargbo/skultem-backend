package com.moriba.skultem.application.dto;

import java.util.List;
import java.util.Map;

// A class master's grade book for a term: every pupil against every subject, from the marks the subject teachers
// have submitted (awaiting approval or approved). Drafts aren't shown - they're still the subject teacher's work.
// classId/sectionId/streamId: the section's class - what the report-card view (the broadsheet) of the same class reads.
public record GradebookDTO(String classSessionId, String className, String termId, String termName, int passMark,
        List<Subject> subjects, List<Student> students, String classId, String sectionId, String streamId) {

    // status: APPROVED (approved or completed), SUBMITTED (awaiting approval) or NOT_SUBMITTED (draft or returned).
    public record Assessment(String id, String name, int position, Integer weight, String status) {
    }

    // submitted: how many of its assessments have been handed in; classAverage over the pupils with a total.
    // code: the subject's short code ("MATH"), for narrow column headings when a class takes 18-20 subjects.
    public record Subject(String teacherSubjectId, String name, String code, String teacher,
            List<Assessment> assessments, int submitted, Double classAverage) {
    }

    // total: the weighted average of the submitted assessments so far (null when none are in); scores by assessment id.
    public record SubjectResult(Double total, String grade, Map<String, Integer> scores) {
    }

    // subjects by teacherSubjectId; average over the subjects with a total; position by average, ties share a place.
    public record Student(String studentId, String name, String admissionNumber, String photo,
            Map<String, SubjectResult> subjects, Double average, Integer position, Boolean passed) {
    }
}
