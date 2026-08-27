package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ReportCardSetting;
import com.moriba.skultem.infrastructure.persistence.entity.ReportCardSettingEntity;

public class ReportCardSettingMapper {
    public static ReportCardSetting toDomain(ReportCardSettingEntity param) {
        if (param == null) return null;

        return new ReportCardSetting(param.getId(), param.getSchoolId(), param.getHeaderColor(), param.getLogoUrl(),
                param.getFooterNote(), param.isShowAttendance(), param.isShowRemarks(), param.isShowPosition(),
                param.isShowSignatures(), param.isShowGradeScale(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ReportCardSettingEntity toEntity(ReportCardSetting param) {
        if (param == null) return null;

        return ReportCardSettingEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .headerColor(param.getHeaderColor())
                .logoUrl(param.getLogoUrl())
                .footerNote(param.getFooterNote())
                .showAttendance(param.isShowAttendance())
                .showRemarks(param.isShowRemarks())
                .showPosition(param.isShowPosition())
                .showSignatures(param.isShowSignatures())
                .showGradeScale(param.isShowGradeScale())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
