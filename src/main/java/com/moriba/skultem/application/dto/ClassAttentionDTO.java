package com.moriba.skultem.application.dto;

import java.util.List;

// Result of ComputeClassAttentionUseCase. students lists everyone who needs a look - including
// students on "watch" (a recent dip the term as a whole doesn't back up) - but flaggedCount counts
// only those at NEEDS_ATTENTION or CRITICAL, which is what the class badge and dashboard report.
public record ClassAttentionDTO(
        int totalStudents,
        int flaggedCount,
        int watchCount,
        List<StudentAttentionDTO> students) {
}
