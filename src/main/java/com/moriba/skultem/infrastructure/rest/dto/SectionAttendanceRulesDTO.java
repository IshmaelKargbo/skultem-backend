package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Every field is optional: null clears that override and the section uses the school's value.
public record SectionAttendanceRulesDTO(
        @DecimalMin(value = "0", message = "Attendance threshold cannot be less than 0") @DecimalMax(value = "100", message = "Attendance threshold cannot be greater than 100") Double attendanceThreshold,
        @Min(value = 5, message = "Attendance window must be at least 5 school days") @Max(value = 60, message = "Attendance window cannot exceed 60 school days") Integer attendanceWindowDays,
        @Min(value = 1, message = "Minimum recorded days must be at least 1") @Max(value = 60, message = "Minimum recorded days cannot exceed 60") Integer attendanceMinDays,
        @Min(value = 2, message = "Consecutive absences must be at least 2") @Max(value = 10, message = "Consecutive absences cannot exceed 10") Integer attendanceStreakDays) {
}
