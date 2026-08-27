package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.IdCardSettingDTO;
import com.moriba.skultem.domain.model.IdCardSetting;

public class IdCardSettingMapper {
    public static IdCardSettingDTO toDTO(IdCardSetting param) {
        return new IdCardSettingDTO(
                param.getLayout(),
                param.getProfileShape(),
                param.getHeaderColor(),
                param.getFooterColor(),
                param.getHeaderTextColor(),
                param.getPrimaryTextColor(),
                param.getWidthMm(),
                param.getHeightMm(),
                param.getBgImageUrl(),
                param.getBgOpacity(),
                param.getSchoolName(),
                param.getSchoolAddress(),
                param.getPrincipalName(),
                param.getFields(),
                param.getValidityYears());
    }
}
