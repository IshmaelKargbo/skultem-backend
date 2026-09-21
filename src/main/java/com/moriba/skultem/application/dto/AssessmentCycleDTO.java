package com.moriba.skultem.application.dto;

/**
 * One assessment of a subject's term, with where it stands in the grading workflow. {@code returnReason}
 * is only set while the assessment is RETURNED: the note the approver left when sending it back, so the
 * person grading can see what needs correcting. Null otherwise.
 */
public record AssessmentCycleDTO(String id, String name, double weight, int position, String status,
        String returnReason) {
}
