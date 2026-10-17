package com.moriba.skultem.infrastructure.rest.dto;

import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

import jakarta.validation.constraints.NotNull;

public record SaveAssessmentConfigurationDTO(
        @NotNull(message = "Choose an assessment structure") AssessmentStructure structure,
        Integer caPercentage,
        Integer formalPercentage,
        CaFrequency caFrequency,
        Integer caEntries,
        // Per-term, per-assessment overrides (which assessments use CA + formal test in which term, and how many CA
        // recordings each has). Anything not listed follows the defaults above.
        java.util.List<PlanItem> plan,
        // Also move assessments nobody has graded yet onto the new setup (default: yes).
        Boolean applyToUnstarted,
        // Optional: split the CA into named parts that, with the formal test, make 100% of the assessment
        // (Attendance 20 + Assignment 20 + Participation 20 + Test 40). Their total must equal caPercentage.
        java.util.List<Component> caComponents,
        // The name of the formal part ("Test"), shown in place of "Formal test".
        String formalLabel) {

    public record Component(@jakarta.validation.constraints.NotBlank String name, int weight, String source) {
    }

    public record PlanItem(String termId, String assessmentName, boolean usesCa, int caEntries,
            java.util.List<Component> caComponents, String formalLabel) {
    }
}
