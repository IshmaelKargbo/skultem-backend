package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.domain.model.AcademicYear;

public class AcademicYearMapper {
    public static AcademicYearDTO toDTO(AcademicYear param) {
        return new AcademicYearDTO(param.getId(), param.getSchoolId(), param.getName(), param.getStartDate(),
                param.getEndDate(), param.isActive(), param.getStatus().toString(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
