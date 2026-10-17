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
        java.util.List<PlanItem> plan,
        // Optional: the CA split into named, weighted parts (each a share of the whole assessment) and the name of
        // the formal part. Empty = the CA is caEntries equal recordings.
        java.util.List<Component> caComponents,
        String formalLabel,
        // Set when this is an assessment template's own configuration.
        String assessmentTemplateId) {

    public record Component(String name, int weight, String source) {
    }

    public record PlanItem(String termId, String assessmentName, boolean usesCa, int caEntries,
            java.util.List<Component> caComponents, String formalLabel) {
    }
}
