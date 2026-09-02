package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.PayslipSetting;
import com.moriba.skultem.infrastructure.persistence.entity.PayslipSettingEntity;

public class PayslipSettingMapper {
    public static PayslipSetting toDomain(PayslipSettingEntity param) {
        if (param == null) {
            return null;
        }

        return new PayslipSetting(param.getId(), param.getSchoolId(), param.getAccentColor(), param.getLogoUrl(),
                param.getFooterNote(), param.isShowWatermark(), param.isShowAmountInWords(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static PayslipSettingEntity toEntity(PayslipSetting param) {
        if (param == null) {
            return null;
        }

        return PayslipSettingEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .accentColor(param.getAccentColor())
                .logoUrl(param.getLogoUrl())
                .footerNote(param.getFooterNote())
                .showWatermark(param.isShowWatermark())
                .showAmountInWords(param.isShowAmountInWords())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
