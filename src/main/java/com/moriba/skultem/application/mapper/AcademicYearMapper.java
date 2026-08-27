package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.domain.model.AcademicYear;

public class AcademicYearMapper {
    public static AcademicYearDTO toDTO(AcademicYear param) {
        if (param == null)
            return null;

        var nextYear = param.getNextYear();

        return new AcademicYearDTO(param.getId(), param.getSchoolId(), param.getName(), param.getStartDate(),
                param.getEndDate(), param.isActive(), param.getStatus().toString(),
                nextYear != null ? nextYear.getId() : null, nextYear != null ? nextYear.getName() : null,
                param.getCreatedAt(), param.getUpdatedAt());
    }
}
