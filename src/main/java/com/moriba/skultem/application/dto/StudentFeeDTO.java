package com.moriba.skultem.application.dto;

import java.time.Instant;

public record StudentFeeDTO(String id, StudentDTO student, FeeStructureDTO fee,
        EnrollmentDTO enrollment, Instant createdAt, Instant updatedAt) {
}
