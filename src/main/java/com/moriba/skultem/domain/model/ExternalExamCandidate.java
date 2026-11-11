package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * One student entered for an external exam sitting: their index (candidate) number and their score in each of the
 * sitting's subjects. A subject with no score is one they did not sit (shown as absent).
 */
@Getter
public class ExternalExamCandidate extends AggregateRoot<String> {

    private final String schoolId;
    private final String sittingId;
    private final String studentId;
    private String studentName;
    private String admissionNumber;
    private String indexNumber;
    // subjectId -> score (0-100). Insertion order is kept, but the sitting's subject order is what prints.
    private Map<String, Double> scores;
    // subjectId -> grade as released (BECE "2", WASSCE "B3"), for exams whose results are entered as grades.
    private Map<String, String> grades = new LinkedHashMap<>();

    public ExternalExamCandidate(String id, String schoolId, String sittingId, String studentId, String studentName,
            String admissionNumber, String indexNumber, Map<String, Double> scores, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.sittingId = sittingId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.admissionNumber = admissionNumber;
        this.indexNumber = indexNumber;
        this.scores = scores == null ? new LinkedHashMap<>() : new LinkedHashMap<>(scores);
        touch(updatedAt);
    }

    public static ExternalExamCandidate create(String id, String schoolId, String sittingId, String studentId,
            String studentName, String admissionNumber) {
        Instant now = Instant.now();
        return new ExternalExamCandidate(id, schoolId, sittingId, studentId, studentName, admissionNumber, null,
                null, now, now);
    }

    public ExternalExamCandidate withGrades(Map<String, String> grades) {
        this.grades = grades == null ? new LinkedHashMap<>() : new LinkedHashMap<>(grades);
        return this;
    }

    /**
     * New index number and grades, for an exam whose results are entered as grades. Each grade must be one of the
     * exam's (allowedGrades, any case); a blank one means the subject was not sat.
     */
    public void recordGrades(String indexNumber, Map<String, String> newGrades, Set<String> sittingSubjectIds,
            java.util.function.Function<String, String> canonicalGrade) {
        Map<String, String> kept = new LinkedHashMap<>();
        if (newGrades != null) {
            for (var e : newGrades.entrySet()) {
                if (!sittingSubjectIds.contains(e.getKey())) {
                    throw new RuleException("A grade was given for a subject this exam doesn't cover");
                }
                String g = e.getValue() == null ? "" : e.getValue().trim();
                if (g.isEmpty()) {
                    continue;
                }
                String canonical = canonicalGrade.apply(g);
                if (canonical == null) {
                    throw new RuleException("\"" + g + "\" is not one of this exam's grades (" + studentName + ")");
                }
                kept.put(e.getKey(), canonical);
            }
        }
        record(indexNumber, scores, sittingSubjectIds);
        this.grades = kept;
    }

    /** Has any result been entered (a score, or a grade)? */
    public boolean hasResults() {
        return !scores.isEmpty() || !grades.isEmpty();
    }

    /** New index number and scores. Scores for subjects the sitting doesn't have are refused. */
    public void record(String indexNumber, Map<String, Double> newScores, Set<String> sittingSubjectIds) {
        Map<String, Double> kept = new LinkedHashMap<>();
        if (newScores != null) {
            for (var e : newScores.entrySet()) {
                if (!sittingSubjectIds.contains(e.getKey())) {
                    throw new RuleException("A score was given for a subject this exam doesn't cover");
                }
                Double score = e.getValue();
                if (score == null) {
                    continue;
                }
                if (score.isNaN() || score < 0 || score > 100) {
                    throw new RuleException("Scores must be between 0 and 100 (" + studentName + ")");
                }
                kept.put(e.getKey(), score);
            }
        }
        String index = indexNumber == null ? null : indexNumber.trim();
        if (index != null && index.length() > 64) {
            throw new RuleException("Index number is too long (" + studentName + ")");
        }
        this.indexNumber = index == null || index.isEmpty() ? null : index;
        this.scores = kept;
        touch(Instant.now());
    }

    /** Keep the printed name and admission number in step with the student record. */
    public void refreshStudent(String name, String admissionNumber) {
        this.studentName = name;
        this.admissionNumber = admissionNumber;
    }
}
