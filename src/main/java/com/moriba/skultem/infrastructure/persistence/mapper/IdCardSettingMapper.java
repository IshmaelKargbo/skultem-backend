package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.IdCardSetting;
import com.moriba.skultem.infrastructure.persistence.entity.IdCardSettingEntity;

public class IdCardSettingMapper {
    public static IdCardSetting toDomain(IdCardSettingEntity param) {
        if (param == null) return null;

        return new IdCardSetting(param.getId(), param.getSchoolId(), param.getLayout(), param.getProfileShape(),
                param.getHeaderColor(), param.getFooterColor(), param.getHeaderTextColor(),
                param.getPrimaryTextColor(), param.getWidthMm(), param.getHeightMm(), param.getBgImageUrl(),
                param.getBgOpacity(), param.getSchoolName(), param.getSchoolAddress(), param.getPrincipalName(),
                param.getFields(), param.getValidityYears(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static IdCardSettingEntity toEntity(IdCardSetting param) {
        if (param == null) return null;

        return IdCardSettingEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .layout(param.getLayout())
                .profileShape(param.getProfileShape())
                .headerColor(param.getHeaderColor())
                .footerColor(param.getFooterColor())
                .headerTextColor(param.getHeaderTextColor())
                .primaryTextColor(param.getPrimaryTextColor())
                .widthMm(param.getWidthMm())
                .heightMm(param.getHeightMm())
                .bgImageUrl(param.getBgImageUrl())
                .bgOpacity(param.getBgOpacity())
                .schoolName(param.getSchoolName())
                .schoolAddress(param.getSchoolAddress())
                .principalName(param.getPrincipalName())
                .fields(param.getFields())
                .validityYears(param.getValidityYears())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
