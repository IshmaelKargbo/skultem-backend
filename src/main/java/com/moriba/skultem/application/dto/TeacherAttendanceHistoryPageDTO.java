package com.moriba.skultem.application.dto;

import java.util.List;

public record TeacherAttendanceHistoryPageDTO(List<TeacherAttendanceDaySummaryDTO> data, long total) {
}
