package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ReceiptSettingDTO;
import com.moriba.skultem.domain.model.ReceiptSetting;

public class ReceiptSettingMapper {
    public static ReceiptSettingDTO toDTO(ReceiptSetting param) {
        return new ReceiptSettingDTO(
                param.getAccentColor(),
                param.getLogoUrl(),
                param.getFooterNote(),
                param.isShowWatermark(),
                param.isShowAmountInWords());
    }
}
