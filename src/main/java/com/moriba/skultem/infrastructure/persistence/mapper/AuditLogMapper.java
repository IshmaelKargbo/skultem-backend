package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.AuditLogEntity;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;
import com.moriba.skultem.infrastructure.persistence.entity.UserSessionEntity;

public class AuditLogMapper {
    public static AuditLog toDomain(AuditLogEntity param) {
        User user = UserMapper.toDomain(param.getUser());
        AcademicYear academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());

        return new AuditLog(param.getId(), param.getAction(), param.getSchoolId(), academicYear, user, param.getIpAddress(),
                param.getDevice(), param.getDeviceType(), param.getOs(), param.getBrowser(), param.getStatus(), param.getDetails(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static AuditLogEntity toEntity(AuditLog param) {
        UserEntity user = UserMapper.toEntity(param.getUser());
        AcademicYearEntity AcademicYear = AcademicYearMapper.toEntity(param.getAcademicYear());

        return AuditLogEntity.builder()
                .id(param.getId())
                .browser(param.getBrowser())
                .action(param.getAction())
                .device(param.getDevice())
                .user(user)
                .academicYear(AcademicYear)
                .ipAddress(param.getIpAddress())
                .schoolId(param.getSchoolId())
                .deviceType(param.getDeviceType())
                .details(param.getDetails())
                .os(param.getOs())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
