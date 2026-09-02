package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;

public class AttendanceLocationSettingMapper {
    public static AttendanceLocationSettingDTO toDTO(AttendanceLocationSetting param) {
        if (param == null) {
            return new AttendanceLocationSettingDTO(false, 0, 0, 150, null);
        }

        return new AttendanceLocationSettingDTO(true, param.getLatitude(), param.getLongitude(),
                param.getRadiusMeters(), param.getAllowedIps());
    }
}
