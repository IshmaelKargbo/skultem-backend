package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// A class's results for one term, every student against every subject - read from the generated report cards,
// so it shows exactly what the cards (and parents) show.
public record BroadsheetDTO(String classId, String className, String termId, String termName, List<String> subjects,
        List<Row> rows, List<SubjectSummary> subjectSummaries, int studentCount, int passed, double classAverage,
        Instant lastGeneratedAt) {

    public record Cell(Double score, String grade) {
    }

    // position: rank by average among the students on this broadsheet (ties share a place).
    public record Row(String studentId, String reportCardId, String studentName, String admissionNumber,
            Map<String, Cell> scores, double average, int position, String overallGrade, boolean passed) {
    }

    public record SubjectSummary(String subject, int sat, double average, Double highest, Double lowest,
            int passed) {
    }
}
