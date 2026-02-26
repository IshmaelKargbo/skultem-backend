package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.dto.HolidayDTO;
import com.moriba.skultem.domain.model.Holiday;

public class HolidayMapper {
    public static HolidayDTO toDTO(Holiday param) {
        AcademicYearDTO academicYear = null;
        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toDTO(param.getAcademicYear());
        }

        return new HolidayDTO(
                param.getId(),
                param.getSchoolId(),
                param.getName(),
                param.getKind(),
                param.getDate(),
                academicYear,
                param.isFixed(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
