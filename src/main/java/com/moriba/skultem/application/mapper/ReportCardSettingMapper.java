package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.domain.model.ReportCardSetting;

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
                param.isShowGradeScale());
    }
}
