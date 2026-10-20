package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeEnrollmentTypeDTO(
        @NotBlank(message = "Enrollment type is required") @Pattern(regexp = "NEW|TRANSFER|RE_ENROLLMENT|EXISTING", message = "Enrollment type must be NEW, TRANSFER, RE_ENROLLMENT or EXISTING") String enrollmentType) {
}
