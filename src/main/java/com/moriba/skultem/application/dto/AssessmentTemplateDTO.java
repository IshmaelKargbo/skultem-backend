package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record AssessmentTemplateDTO(String id, String name, String description, int passMark, List<AssessmentDTO> assessments,
        Instant createdAt, Instant updatedAt, Scoring scoring, int startsInTerm) {

    // How the template scores its tests, for the list: plain scores, or continuous assessment (CA) and what it is
    // made of. `inherited` = the template has no setup of its own and follows the school-wide one.
    public record Scoring(String structure, int caPercentage, int formalPercentage, int parts, boolean attendance,
            int overrides, boolean inherited, int caTests) {
        public static Scoring simple() {
            return new Scoring("SIMPLE", 0, 100, 0, false, 0, false, 0);
        }
    }
}
