package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.services.RemarkScale;
import com.moriba.skultem.domain.model.ReportCardSetting;
import com.moriba.skultem.domain.vo.RemarkBand;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

public class ReportCardSettingMapper {
    public static ReportCardSettingDTO toDTO(ReportCardSetting param) {
        return new ReportCardSettingDTO(
                param.getHeaderColor(),
                param.getLogoUrl(),
                param.getFooterNote(),
                param.isShowAttendance(),
                param.isShowRemarks(),
                param.isShowPosition(),
                param.isShowSignatures(),
                param.isShowTeacherSignature(),
                param.isShowPrincipalSignature(),
                param.isShowGradeScale(),
                remarkScale(param));
    }

    // A NULL column means the school never set a scale, so it gets the defaults; an explicitly saved empty
    // list ("[]") means the school deleted them all and wants no automatic remarks.
    public static List<RemarkBand> remarkScale(ReportCardSetting param) {
        if (param.getRemarkScale() == null) {
            return RemarkScale.defaults();
        }

        var bands = JsonMapper.fromJson(param.getRemarkScale(), new TypeReference<List<RemarkBand>>() {
        });
        return bands == null ? List.of() : bands;
    }
}
