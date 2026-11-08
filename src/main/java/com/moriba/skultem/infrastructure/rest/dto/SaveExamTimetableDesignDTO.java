package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveExamTimetableDesignDTO(
        @Size(max = 120, message = "Keep the heading under 120 characters") String heading,
        String accentColor,
        @Pattern(regexp = "PORTRAIT|LANDSCAPE", message = "Orientation must be PORTRAIT or LANDSCAPE") String orientation,
        boolean showLogo,
        boolean showVenue,
        boolean showDuration,
        boolean showInvigilators,
        boolean showInstructions,
        boolean showSignatures,
        @Size(max = 60) String leftSignature,
        @Size(max = 60) String rightSignature,
        @Size(max = 255) String footerNote) {
}
