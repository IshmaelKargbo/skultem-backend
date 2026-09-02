package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherAttendanceEntity;

public class TeacherAttendanceMapper {
    public static TeacherAttendance toDomain(TeacherAttendanceEntity param) {
        if (param == null) {
            return null;
        }

        return new TeacherAttendance(
                param.getId(),
                param.getSchoolId(),
                TeacherMapper.toDomain(param.getTeacher()),
                param.getDate(),
                param.getStatus(),
                param.getNote(),
                param.getClockedInAt(),
                param.getClockInIp(),
                param.getClockedOutAt(),
                param.getClockOutIp(),
                param.isClockInByAdmin(),
                param.isClockOutByAdmin(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static TeacherAttendanceEntity toEntity(TeacherAttendance param) {
        if (param == null) {
            return null;
        }

        return TeacherAttendanceEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .teacher(TeacherMapper.toEntity(param.getTeacher()))
                .date(param.getDate())
                .status(param.getStatus())
                .note(param.getNote())
                .clockedInAt(param.getClockedInAt())
                .clockInIp(param.getClockInIp())
                .clockedOutAt(param.getClockedOutAt())
                .clockOutIp(param.getClockOutIp())
                .clockInByAdmin(param.isClockInByAdmin())
                .clockOutByAdmin(param.isClockOutByAdmin())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
