package com.moriba.skultem.application.dto;

import java.util.List;

// School-wide (or, for a teacher, class-master-scoped) rollup of ComputeClassAttentionUseCase
// across every class - backs the "Needs Attention" dashboard widget. classes is capped to the
// most urgent few (see ListClassesNeedingAttentionUseCase), not every flagged class.
public record ClassAttentionSummaryDTO(
        int totalClasses,
        int flaggedClasses,
        int flaggedStudents,
        List<FlaggedClassDTO> classes) {
}
