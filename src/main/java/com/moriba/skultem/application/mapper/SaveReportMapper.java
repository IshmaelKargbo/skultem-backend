package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SaveReportDTO;
import com.moriba.skultem.domain.model.SaveReport;

public class SaveReportMapper {
    public static SaveReportDTO toDTO(SaveReport param) {
        return new SaveReportDTO(
                param.getId(),
                param.getName(),
                param.getEntity(),
                param.getFilters(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
