package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TimetableSettingDTO;
import com.moriba.skultem.domain.model.TimetableSetting;

public class TimetableSettingMapper {
    public static TimetableSettingDTO toDTO(TimetableSetting param) {
        if (param == null) {
            return null;
        }

        return new TimetableSettingDTO(param.getTitle(), param.getAccentColor(), param.getOrientation(),
                param.isShowLogo(), param.isShowIcons(), param.isShowTeacher(), param.isShowRoom(),
                param.isShowPeriodTimes(), param.getFooterNote());
    }
}
