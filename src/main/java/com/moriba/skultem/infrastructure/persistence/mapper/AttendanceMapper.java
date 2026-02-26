package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.infrastructure.persistence.entity.AttendanceEntity;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;

public class AttendanceMapper {
    public static Attendance toDomain(AttendanceEntity param) {
        Enrollment enrollment = null;

        if (param.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toDomain(param.getEnrollment());
        }

        return new Attendance(param.getId(), param.getSchoolId(), enrollment, param.getDate(), param.isPresent(),
                param.isExcused(), param.isLate(), param.getReason(), param.isHoliday(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static AttendanceEntity toEntity(Attendance args) {
        EnrollmentEntity enrollment = null;

        if (args.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toEntity(args.getEnrollment());
        }

        return AttendanceEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .enrollment(enrollment)
                .date(args.getDate())
                .present(args.isPresent())
                .excused(args.isExcused())
                .late(args.isLate())
                .reason(args.getReason())
                .holiday(args.isHoliday())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
