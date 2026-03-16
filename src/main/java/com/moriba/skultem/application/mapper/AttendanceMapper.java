package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.domain.model.Attendance;

public class AttendanceMapper {
    public static AttendanceDTO toDTO(Attendance param) {
        if (param.getEnrollment() == null) {
            return null;
        }

        EnrollmentDTO enrollment = EnrollmentMapper.toDTO(param.getEnrollment());
        String name = String.join(" ", enrollment.student().givenNames(), enrollment.student().familyName());
        return new AttendanceDTO(
                param.getId(),
                enrollment.student().id(),
                name,
                enrollment.student().className(),
                param.getDate(),
                param.getState(),
                param.getReason(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
