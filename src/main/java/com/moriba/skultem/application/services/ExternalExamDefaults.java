package com.moriba.skultem.application.services;

import java.util.List;

import com.moriba.skultem.domain.model.ExternalExam.AggregateMode;
import com.moriba.skultem.domain.vo.ExamGrade;
import com.moriba.skultem.domain.vo.Level;

/**
 * The national exams every school starts with. The scales are starting values a school can (and should) check
 * against the exam body's current published scale and edit on the Exams page; nothing here is fixed in code after
 * the school's copy is made.
 */
public final class ExternalExamDefaults {

    public record Default(String code, String name, String examBody, Level level, List<ExamGrade> scale,
            AggregateMode mode, int bestOf) {
    }

    private ExternalExamDefaults() {
    }

    /** BECE and WASSCE results are released as grades (1-7, A1-F9), so they are entered as grades; NPSE as scores. */
    public static com.moriba.skultem.domain.model.ExternalExam.EntryMode entryMode(String code) {
        return "BECE".equals(code) || "WASSCE".equals(code)
                ? com.moriba.skultem.domain.model.ExternalExam.EntryMode.GRADES
                : com.moriba.skultem.domain.model.ExternalExam.EntryMode.SCORES;
    }

    public static List<Default> all() {
        return List.of(
                new Default("NPSE", "National Primary School Examination", "MBSSE", Level.PRIMARY, List.of(
                        new ExamGrade(80, 100, "A", 1, "Excellent"),
                        new ExamGrade(70, 79, "B", 2, "Very good"),
                        new ExamGrade(60, 69, "C", 3, "Good"),
                        new ExamGrade(50, 59, "D", 4, "Credit"),
                        new ExamGrade(40, 49, "E", 5, "Pass"),
                        new ExamGrade(0, 39, "F", 6, "Fail")),
                        AggregateMode.TOTAL, 0),
                new Default("BECE", "Basic Education Certificate Examination", "WAEC", Level.JSS, List.of(
                        new ExamGrade(75, 100, "1", 1, "Excellent"),
                        new ExamGrade(65, 74, "2", 2, "Very good"),
                        new ExamGrade(55, 64, "3", 3, "Good"),
                        new ExamGrade(50, 54, "4", 4, "Credit"),
                        new ExamGrade(45, 49, "5", 5, "Pass"),
                        new ExamGrade(40, 44, "6", 6, "Weak pass"),
                        new ExamGrade(0, 39, "7", 7, "Fail")),
                        AggregateMode.POINTS, 6),
                new Default("WASSCE", "West African Senior School Certificate Examination", "WAEC", Level.SSS, List.of(
                        new ExamGrade(75, 100, "A1", 1, "Excellent"),
                        new ExamGrade(70, 74, "B2", 2, "Very good"),
                        new ExamGrade(65, 69, "B3", 3, "Good"),
                        new ExamGrade(60, 64, "C4", 4, "Credit"),
                        new ExamGrade(55, 59, "C5", 5, "Credit"),
                        new ExamGrade(50, 54, "C6", 6, "Credit"),
                        new ExamGrade(45, 49, "D7", 7, "Pass"),
                        new ExamGrade(40, 44, "E8", 8, "Pass"),
                        new ExamGrade(0, 39, "F9", 9, "Fail")),
                        AggregateMode.POINTS, 6));
    }
}
