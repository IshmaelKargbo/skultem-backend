package com.moriba.skultem.application.dto;

// payload is one of TeacherAttendanceRosterDTO, List<TeacherAttendanceSummaryRowDTO> or
// TermTeacherAttendanceSummaryDTO depending on reportType - internal school management document,
// not an MBSSE/Ministry form (no such claim is made anywhere this renders).
public record TeacherManagementReportDTO(TeacherManagementReportType reportType, Object payload) {
}
