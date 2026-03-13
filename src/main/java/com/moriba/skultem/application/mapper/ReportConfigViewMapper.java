package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ReportConfigDTO;
import com.moriba.skultem.domain.model.ReportConfig;

public class ReportConfigViewMapper {
    public static ReportConfigDTO toDTO(ReportConfig param) {
        return new ReportConfigDTO(
                param.getId(),
                param.getName(),
                param.getType(),
                param.getFormat(),
                param.getClassId(),
                param.getClassSessionId(),
                param.getTeacherSubjectId(),
                param.getTermId(),
                param.getStartDate(),
                param.getEndDate(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
