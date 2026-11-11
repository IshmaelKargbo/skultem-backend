package com.moriba.skultem.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject;
import com.moriba.skultem.domain.vo.ExamGrade;

/**
 * Reads a candidate's scores on an exam's grade scale: a grade and points per subject, and the aggregate - the
 * points of the best N subjects (fewer is better) or the total score, as the exam says.
 */
public final class ExternalExamResults {

    public record SubjectResult(String subjectId, String subject, Double score, String grade, Integer points,
            String remark) {
        public boolean absent() {
            return "ABS".equals(grade) && score == null;
        }
    }

    public record Result(List<SubjectResult> subjects, int subjectsSat, double totalScore, Integer aggregate,
            String aggregateLabel) {
    }

    /** The grade entered for a subject a candidate takes but was absent from. */
    public static final String ABSENT = "ABS";

    private ExternalExamResults() {
    }

    public static ExamGrade gradeFor(List<ExamGrade> scale, double score) {
        int rounded = (int) Math.round(score);
        return scale.stream().filter(g -> rounded >= g.minScore() && rounded <= g.maxScore()).findFirst()
                .orElse(null);
    }

    public static Result of(ExternalExam exam, List<ExamSubject> subjects, Map<String, Double> scores) {
        return of(exam, subjects, scores, Map.of());
    }

    /** As above; for an exam whose results are entered as grades, from the grades (points and remark from the scale). */
    public static Result of(ExternalExam exam, List<ExamSubject> subjects, Map<String, Double> scores,
            Map<String, String> grades) {
        if (exam.gradesEntered()) {
            return fromGrades(exam, subjects, grades == null ? Map.of() : grades);
        }
        List<SubjectResult> rows = new ArrayList<>();
        double total = 0;
        int sat = 0;
        for (var s : subjects) {
            Double score = scores == null ? null : scores.get(s.subjectId());
            if (score == null) {
                rows.add(new SubjectResult(s.subjectId(), s.name(), null, "ABS", null, "Absent"));
                continue;
            }
            sat++;
            total += score;
            var g = gradeFor(exam.getGradeScale(), score);
            rows.add(new SubjectResult(s.subjectId(), s.name(), score, g == null ? "-" : g.grade(),
                    g == null ? null : g.points(), g == null ? "" : g.remark()));
        }

        Integer aggregate;
        String label;
        if (exam.getAggregateMode() == ExternalExam.AggregateMode.TOTAL) {
            aggregate = sat == 0 ? null : (int) Math.round(total);
            label = "Total score";
        } else {
            List<Integer> points = rows.stream().map(SubjectResult::points).filter(p -> p != null)
                    .sorted(Comparator.naturalOrder()).toList();
            int n = exam.getBestOf() > 0 ? Math.min(exam.getBestOf(), points.size()) : points.size();
            aggregate = n == 0 ? null : points.subList(0, n).stream().mapToInt(Integer::intValue).sum();
            label = exam.getBestOf() > 0 ? "Aggregate (best " + exam.getBestOf() + ")" : "Aggregate";
        }
        return new Result(rows, sat, total, aggregate, label);
    }

    private static Result fromGrades(ExternalExam exam, List<ExamSubject> subjects, Map<String, String> grades) {
        List<SubjectResult> rows = new ArrayList<>();
        int sat = 0;
        for (var s : subjects) {
            String grade = grades.get(s.subjectId());
            // Empty: the candidate doesn't take this subject (an Art student and Physics), so it isn't on their
            // result at all. "ABS": they take it but were absent.
            if (grade == null || grade.isBlank()) {
                continue;
            }
            if (ABSENT.equalsIgnoreCase(grade.trim())) {
                rows.add(new SubjectResult(s.subjectId(), s.name(), null, ABSENT, null, "Absent"));
                continue;
            }
            sat++;
            var band = exam.band(grade);
            rows.add(new SubjectResult(s.subjectId(), s.name(), null, band == null ? grade : band.grade(),
                    band == null ? null : band.points(), band == null ? "" : band.remark()));
        }
        // Released grades carry points, so the aggregate is always the points of the best N subjects.
        List<Integer> points = rows.stream().map(SubjectResult::points).filter(p -> p != null)
                .sorted(Comparator.naturalOrder()).toList();
        int n = exam.getBestOf() > 0 ? Math.min(exam.getBestOf(), points.size()) : points.size();
        Integer aggregate = n == 0 ? null : points.subList(0, n).stream().mapToInt(Integer::intValue).sum();
        String label = exam.getBestOf() > 0 ? "Aggregate (best " + exam.getBestOf() + ")" : "Aggregate";
        return new Result(rows, sat, 0, aggregate, label);
    }
}
