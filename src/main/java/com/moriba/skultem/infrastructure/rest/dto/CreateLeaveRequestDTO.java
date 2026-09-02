package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import com.moriba.skultem.domain.model.LeaveRequest.Type;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLeaveRequestDTO(

        // Only read for the admin-facing /leave endpoint - ignored by /leave/me, which always
        // files the request under the caller's own teacher record.
        String teacherId,

        @NotNull(message = "Leave type is required")
        Type type,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotBlank(message = "Reason is required")
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason

) {
}
