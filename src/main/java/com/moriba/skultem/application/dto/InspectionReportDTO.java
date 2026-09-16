package com.moriba.skultem.application.dto;

// payload is one of ClassSessionAttendanceDTO, List<StudentAttendanceSummaryDTO> or
// TermAttendanceSummaryDTO depending on reportType - the frontend already branches on reportType
// to pick a printable template, so this stays a thin envelope rather than three parallel DTOs.
public record InspectionReportDTO(InspectionReportType reportType, Object payload) {
}
