package com.moriba.skultem.application.dto;

import java.util.List;

// Result of ComputeClassAttentionUseCase - students is every enrolled student who was flagged
// (attendance and/or academics), not the whole roster.
public record ClassAttentionDTO(
        int totalStudents,
        int flaggedCount,
        List<StudentAttentionDTO> students) {
}
