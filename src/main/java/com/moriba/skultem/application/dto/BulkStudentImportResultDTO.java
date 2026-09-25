package com.moriba.skultem.application.dto;

import java.util.List;

public record BulkStudentImportResultDTO(
        boolean dryRun,
        int ready,
        int created,
        int failed,
        List<Row> rows) {

    public record Row(
            int row,
            String name,
            String className,
            String admissionNumber,
            String guardianPhone,
            boolean existingGuardian,
            String outcome,
            String message,
            // Set when the row failed on its subjects: the elective groups of its class to pick from.
            List<SubjectChoice> subjectChoices) {
    }

    public record SubjectChoice(
            String group,
            int select,
            List<String> subjects) {
    }
}
