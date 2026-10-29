package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveLibrarySettingDTO(
        @NotNull(message = "Student limit is required") @Min(value = 1, message = "Students must be allowed at least 1 book") @Max(value = 50, message = "Student limit can't be more than 50") Integer maxStudentLoans,
        @NotNull(message = "Teacher limit is required") @Min(value = 1, message = "Teachers must be allowed at least 1 book") @Max(value = 50, message = "Teacher limit can't be more than 50") Integer maxTeacherLoans,
        @NotNull(message = "Overdue rule is required") Boolean blockWhenOverdue,
        @NotNull(message = "Loan period is required") @Min(value = 1, message = "Loan period must be at least 1 day") @Max(value = 365, message = "Loan period can't be more than 365 days") Integer defaultLoanDays) {
}
