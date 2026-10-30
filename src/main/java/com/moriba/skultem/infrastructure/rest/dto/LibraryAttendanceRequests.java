package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Request bodies for LibraryAttendanceController.
public final class LibraryAttendanceRequests {

    private static final String FINAL_STATUS = "PRESENT|ABSENT|EXCUSED";
    private static final String STATUS_MESSAGE = "Status must be PRESENT, ABSENT or EXCUSED";

    private LibraryAttendanceRequests() {
    }

    public record CheckIn(
            @NotEmpty(message = "Select at least one student") @Size(max = 100, message = "Check in at most 100 students at a time") List<String> studentIds,
            Boolean eligibleOnly) {
    }

    public record Reason(
            @NotBlank(message = "A reason is required") @Size(max = 500, message = "Keep the reason under 500 characters") String reason) {
    }

    public record SessionSlot(
            @NotBlank(message = "Library period is required") String timetableEntryId,
            @NotNull(message = "Date is required") LocalDate date) {
    }

    public record CancelSlot(
            @NotBlank(message = "Library period is required") String timetableEntryId,
            @NotNull(message = "Date is required") LocalDate date,
            @NotBlank(message = "A reason is required") @Size(max = 500, message = "Keep the reason under 500 characters") String reason) {
    }

    public record Mark(
            @NotBlank(message = "Student is required") String recordId,
            @NotBlank(message = STATUS_MESSAGE) @Pattern(regexp = FINAL_STATUS, message = STATUS_MESSAGE) String status,
            @Size(max = 500, message = "Keep the reason under 500 characters") String excuseReason) {
    }

    public record Marks(@NotEmpty(message = "Nothing to save") List<@Valid Mark> marks) {
    }

    public record AddToRoster(
            @NotBlank(message = "Student is required") String studentId,
            @NotBlank(message = "A reason is required") @Size(max = 500, message = "Keep the reason under 500 characters") String reason) {
    }

    public record Correct(
            @NotBlank(message = STATUS_MESSAGE) @Pattern(regexp = FINAL_STATUS, message = STATUS_MESSAGE) String status,
            @Size(max = 500, message = "Keep the reason under 500 characters") String excuseReason,
            @NotBlank(message = "A reason is required for a correction") @Size(max = 500, message = "Keep the reason under 500 characters") String reason) {
    }
}
