package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;

public class AttendanceLocationSettingMapper {
    public static AttendanceLocationSettingDTO toDTO(AttendanceLocationSetting param) {
        return toDTO(param, null);
    }

    // For a section that has no location of its own yet: an unconfigured placeholder for that section.
    public static AttendanceLocationSettingDTO toDTO(AttendanceLocationSetting param, String managementSectionId) {
        // A stored 0,0 (saved before this was refused) is not a location - treat it as not set up.
        if (param == null || !param.isSet()) {
            return new AttendanceLocationSettingDTO(false, 0, 0, 150, null,
                    managementSectionId != null || param == null ? managementSectionId : param.getManagementSectionId());
        }

        return new AttendanceLocationSettingDTO(true, param.getLatitude(), param.getLongitude(),
                param.getRadiusMeters(), param.getAllowedIps(), param.getManagementSectionId());
    }
}
