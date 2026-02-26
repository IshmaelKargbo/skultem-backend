package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.domain.model.Attendance;

public class AttendanceMapper {
    public static AttendanceDTO toDTO(Attendance param) {
        EnrollmentDTO enrollment = null;
        if (param.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toDTO(param.getEnrollment());
        }

        return new AttendanceDTO(
                param.getId(),
                param.getSchoolId(),
                enrollment,
                param.getDate(),
                param.isPresent(),
                param.isExcused(),
                param.isLate(),
                param.getReason(),
                param.isHoliday(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
