package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.moriba.skultem.domain.vo.ExamGrade;

// What the External Exams endpoints send and take.
public final class ExternalExamDTO {

    private ExternalExamDTO() {
    }

    public record Exam(String id, String code, String name, String examBody, String level,
            List<ExamGrade> gradeScale, String aggregateMode, int bestOf, boolean builtIn,
            // The exam's own print designs; null = the school's general exam design.
            String slipTemplate, String statementTemplate,
            // SCORES (a score per subject) or GRADES (the grade the exam body released, e.g. BECE 1-7).
            String entryMode) {
    }

    public record SaveExam(String code, String name, String examBody, String level, List<ExamGrade> gradeScale,
            String aggregateMode, Integer bestOf, String entryMode) {
    }

    public record SubjectOption(String id, String name) {
    }

    public record SittingSummary(String id, String examId, String examCode, String examName, String classId,
            String className, String academicYearId, String academicYearName, String centreNumber, String status,
            int candidates, int withIndexNumber, int scoresEntered, int scoresExpected, Instant confirmedAt,
            String confirmedByName) {
    }

    public record CreateSitting(String examId, String classId, String academicYearId, String centreNumber,
            List<String> subjectIds) {
    }

    public record UpdateSitting(String centreNumber, List<String> subjectIds) {
    }

    public record SubjectResult(String subjectId, String subject, Double score, String grade, Integer points,
            String remark) {
    }

    public record Candidate(String id, String studentId, String studentName, String admissionNumber,
            String indexNumber, Map<String, Double> scores, Map<String, String> grades, List<SubjectResult> results,
            int subjectsSat, Integer aggregate) {
    }

    public record Sitting(SittingSummary summary, Exam exam, List<SubjectOption> subjects, String aggregateLabel,
            List<Candidate> candidates) {
    }

    // scores for an exam entered as scores, grades for one entered as grades.
    public record CandidateScores(String candidateId, String indexNumber, Map<String, Double> scores,
            Map<String, String> grades) {
    }

    public record SaveScores(List<CandidateScores> candidates) {
    }
}
