package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.TimetableSetting;
import com.moriba.skultem.infrastructure.persistence.entity.TimetableSettingEntity;

public class TimetableSettingMapper {
    public static TimetableSetting toDomain(TimetableSettingEntity param) {
        if (param == null) {
            return null;
        }

        return new TimetableSetting(param.getId(), param.getSchoolId(), param.getTitle(), param.getAccentColor(),
                param.getOrientation(), param.isShowLogo(), param.isShowIcons(), param.isShowTeacher(),
                param.isShowRoom(), param.isShowPeriodTimes(), param.getFooterNote(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static TimetableSettingEntity toEntity(TimetableSetting param) {
        if (param == null) {
            return null;
        }

        return TimetableSettingEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .title(param.getTitle())
                .accentColor(param.getAccentColor())
                .orientation(param.getOrientation())
                .showLogo(param.isShowLogo())
                .showIcons(param.isShowIcons())
                .showTeacher(param.isShowTeacher())
                .showRoom(param.isShowRoom())
                .showPeriodTimes(param.isShowPeriodTimes())
                .footerNote(param.getFooterNote())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
