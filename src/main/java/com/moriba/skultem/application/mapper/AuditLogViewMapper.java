package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AuditLogDTO;
import com.moriba.skultem.domain.model.AuditLog;

public class AuditLogViewMapper {
    public static AuditLogDTO toDTO(AuditLog param) {
        String academicYearName = param.getAcademicYear() != null ? param.getAcademicYear().getName() : null;
        String userId = param.getUser() != null ? param.getUser().getId() : null;
        String userName = param.getUser() != null ? param.getUser().getName() : "System";
        String userEmail = param.getUser() != null ? param.getUser().getEmail() : null;

        return new AuditLogDTO(
                param.getId(),
                param.getAction(),
                academicYearName,
                userId,
                userName,
                userEmail,
                param.getIpAddress(),
                param.getDevice(),
                param.getDeviceType(),
                param.getOs(),
                param.getBrowser(),
                param.getStatus(),
                param.getDetails(),
                param.getCreatedAt());
    }
}
