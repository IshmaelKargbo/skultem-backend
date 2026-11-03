package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExamScheduleDTO(String id, String termId, String termName, String classId, String className,
        String level, String subjectId, String subjectName, String paper, LocalDate examDate, LocalTime startTime,
        LocalTime endTime, String venue, String invigilatorId, String invigilatorName, String notes) {
}
