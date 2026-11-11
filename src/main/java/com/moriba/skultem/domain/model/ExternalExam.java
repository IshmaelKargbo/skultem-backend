package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.ExamGrade;
import com.moriba.skultem.domain.vo.Level;

import lombok.Getter;

/**
 * A national (external) exam a school's candidates sit - NPSE, BECE, WASSCE, or one the school adds - with the
 * grade scale its results are read on. Each school has its own copy, so a school can correct a scale; the built-in
 * ones are created the first time the school opens External Exams (see ExternalExamDefaults).
 */
@Getter
public class ExternalExam extends AggregateRoot<String> {

    /** How results are entered: a score per subject (NPSE), or the grade the exam body released (BECE, WASSCE). */
    public enum EntryMode {
        SCORES, GRADES
    }

    /** How a candidate's subjects add up to one figure. */
    public enum AggregateMode {
        // The grade points of the best N subjects (lowest points are best): BECE, WASSCE.
        POINTS,
        // The sum of the scores: NPSE.
        TOTAL
    }

    private final String schoolId;
    private final String code;
    private String name;
    private String examBody;
    private Level level;
    private List<ExamGrade> gradeScale;
    private AggregateMode aggregateMode;
    // How many subjects count toward a POINTS aggregate; 0 = all of them.
    private int bestOf;
    private final boolean builtIn;
    private EntryMode entryMode = EntryMode.SCORES;
    // The exam's own print designs; null = the school's general exam design.
    private String slipTemplate;
    private String statementTemplate;

    public ExternalExam(String id, String schoolId, String code, String name, String examBody, Level level,
            List<ExamGrade> gradeScale, AggregateMode aggregateMode, int bestOf, boolean builtIn, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.code = code;
        this.name = name;
        this.examBody = examBody;
        this.level = level;
        this.gradeScale = gradeScale == null ? List.of() : List.copyOf(gradeScale);
        this.aggregateMode = aggregateMode == null ? AggregateMode.POINTS : aggregateMode;
        this.bestOf = bestOf;
        this.builtIn = builtIn;
        touch(updatedAt);
    }

    public ExternalExam withEntryMode(EntryMode mode) {
        this.entryMode = mode == null ? EntryMode.SCORES : mode;
        return this;
    }

    public void setEntryMode(EntryMode mode) {
        this.entryMode = mode == null ? EntryMode.SCORES : mode;
        touch(Instant.now());
    }

    public boolean gradesEntered() {
        return entryMode == EntryMode.GRADES;
    }

    /** The scale's band for a grade as entered ("2", "b3"), or null when the exam has no such grade. */
    public ExamGrade band(String grade) {
        if (grade == null) {
            return null;
        }
        String g = grade.trim();
        return gradeScale.stream().filter(b -> b.grade().equalsIgnoreCase(g)).findFirst().orElse(null);
    }

    /** Loads the exam's own designs (from storage). */
    public ExternalExam withDesigns(String slipTemplate, String statementTemplate) {
        this.slipTemplate = blankToNull(slipTemplate);
        this.statementTemplate = blankToNull(statementTemplate);
        return this;
    }

    /** The exam's own design for the result slip (statement=false) or the statement; null = use the general one. */
    public String design(boolean statement) {
        return statement ? statementTemplate : slipTemplate;
    }

    public void setDesign(boolean statement, String template) {
        if (statement) {
            this.statementTemplate = blankToNull(template);
        } else {
            this.slipTemplate = blankToNull(template);
        }
        touch(Instant.now());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static ExternalExam create(String id, String schoolId, String code, String name, String examBody,
            Level level, List<ExamGrade> gradeScale, AggregateMode aggregateMode, int bestOf, boolean builtIn) {
        Instant now = Instant.now();
        var exam = new ExternalExam(id, schoolId, normalizeCode(code), required(name, "The exam needs a name"),
                examBody, level, List.of(), aggregateMode, 0, builtIn, now, now);
        exam.setRules(gradeScale, aggregateMode, bestOf);
        return exam;
    }

    public void update(String name, String examBody, Level level, List<ExamGrade> gradeScale,
            AggregateMode aggregateMode, int bestOf) {
        this.name = required(name, "The exam needs a name");
        this.examBody = examBody == null ? null : examBody.trim();
        this.level = level;
        setRules(gradeScale, aggregateMode, bestOf);
        touch(Instant.now());
    }

    private void setRules(List<ExamGrade> scale, AggregateMode mode, int bestOf) {
        if (bestOf < 0 || bestOf > 30) {
            throw new RuleException("Best of must be between 0 (all subjects) and 30");
        }
        this.gradeScale = normalizeScale(scale);
        this.aggregateMode = mode == null ? AggregateMode.POINTS : mode;
        this.bestOf = bestOf;
    }

    /** The scale sorted high to low, checked: every band named, inside 0-100, no overlaps. */
    static List<ExamGrade> normalizeScale(List<ExamGrade> scale) {
        if (scale == null || scale.isEmpty()) {
            throw new RuleException("The exam needs a grade scale");
        }
        List<ExamGrade> sorted = new ArrayList<>();
        for (var g : scale) {
            if (g.grade() == null || g.grade().isBlank()) {
                throw new RuleException("Every grade band needs a grade");
            }
            if (g.minScore() < 0 || g.maxScore() > 100 || g.minScore() > g.maxScore()) {
                throw new RuleException("Grade " + g.grade().trim() + " must be a range within 0 and 100");
            }
            sorted.add(new ExamGrade(g.minScore(), g.maxScore(), g.grade().trim(), g.points(),
                    g.remark() == null ? "" : g.remark().trim()));
        }
        sorted.sort(Comparator.comparingInt(ExamGrade::minScore).reversed());
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).maxScore() >= sorted.get(i - 1).minScore()) {
                throw new RuleException("Grades " + sorted.get(i - 1).grade() + " and " + sorted.get(i).grade()
                        + " overlap");
            }
        }
        return List.copyOf(sorted);
    }

    static String normalizeCode(String code) {
        String c = code == null ? "" : code.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        if (c.isEmpty() || c.length() > 32) {
            throw new RuleException("The exam needs a short code, such as BECE");
        }
        return c;
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuleException(message);
        }
        return value.trim();
    }
}
