package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Recording a continuous-assessment ("CA + formal test") assessment: per student, the CA recordings being
// set and/or the formal test score.
public record RecordContinuousAssessmentDTO(
        @NotNull(message = "Assessment id is required") String assessmentId,
        @NotNull(message = "Term id is required") String termId,
        @NotEmpty(message = "At least one student must be provided") @Valid List<StudentRecord> records) {

    public record StudentRecord(
            @NotNull(message = "Score id is required") String scoreId,
            @Valid List<Entry> entries,
            @Min(value = 0, message = "Score must be between 0 and 100") @Max(value = 100, message = "Score must be between 0 and 100") Integer formalScore) {
    }

    public record Entry(
            @Min(value = 1, message = "Recording number starts at 1") int entryNumber,
            @Min(value = 0, message = "Score must be between 0 and 100") @Max(value = 100, message = "Score must be between 0 and 100") Integer score) {
    }
}
