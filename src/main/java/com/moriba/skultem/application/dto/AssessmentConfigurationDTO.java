package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

// A section's (or the whole school's) assessment configuration. `isDefault` = nothing has been set, so it is
// the standard single-score assessment; `inherited` = it comes from the school-wide configuration rather than
// the section's own.
public record AssessmentConfigurationDTO(
        String managementSectionId,
        String sectionName,
        AssessmentStructure structure,
        boolean caEnabled,
        int caPercentage,
        int formalPercentage,
        CaFrequency caFrequency,
        String caFrequencyUnit,
        int caEntries,
        int version,
        boolean isDefault,
        boolean inherited,
        Instant updatedAt,
        String updatedBy,
        // Per-term, per-assessment overrides: which assessments use CA + formal test in which term, and how many CA
        // recordings each has.
        java.util.List<PlanItem> plan) {

    public record PlanItem(String termId, String assessmentName, boolean usesCa, int caEntries) {
    }
}
