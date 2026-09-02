package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.PayslipSettingDTO;
import com.moriba.skultem.domain.model.PayslipSetting;

public class PayslipSettingMapper {
    public static PayslipSettingDTO toDTO(PayslipSetting param) {
        if (param == null) {
            return null;
        }

        return new PayslipSettingDTO(param.getAccentColor(), param.getLogoUrl(), param.getFooterNote(),
                param.isShowWatermark(), param.isShowAmountInWords());
    }
}
