package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.infrastructure.persistence.entity.AttendanceLocationSettingEntity;

public class AttendanceLocationSettingMapper {
    public static AttendanceLocationSetting toDomain(AttendanceLocationSettingEntity param) {
        if (param == null) {
            return null;
        }

        return new AttendanceLocationSetting(param.getId(), param.getSchoolId(), param.getLatitude(),
                param.getLongitude(), param.getRadiusMeters(), param.getAllowedIps(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static AttendanceLocationSettingEntity toEntity(AttendanceLocationSetting param) {
        if (param == null) {
            return null;
        }

        return AttendanceLocationSettingEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .latitude(param.getLatitude())
                .longitude(param.getLongitude())
                .radiusMeters(param.getRadiusMeters())
                .allowedIps(param.getAllowedIps())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
