package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ReceiptSetting;
import com.moriba.skultem.infrastructure.persistence.entity.ReceiptSettingEntity;

public class ReceiptSettingMapper {
    public static ReceiptSetting toDomain(ReceiptSettingEntity param) {
        if (param == null) return null;

        return new ReceiptSetting(param.getId(), param.getSchoolId(), param.getAccentColor(), param.getLogoUrl(),
                param.getFooterNote(), param.isShowWatermark(), param.isShowAmountInWords(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static ReceiptSettingEntity toEntity(ReceiptSetting param) {
        if (param == null) return null;

        return ReceiptSettingEntity.builder()
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
