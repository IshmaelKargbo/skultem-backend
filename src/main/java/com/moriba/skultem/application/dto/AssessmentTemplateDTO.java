package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record AssessmentTemplateDTO(String id, String name, String description, List<AssessmentDTO> assessments,
        Instant createdAt, Instant updatedAt) {
}
