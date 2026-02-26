package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Holiday;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.HolidayEntity;

public class HolidayMapper {
    public static Holiday toDomain(HolidayEntity param) {
        AcademicYear academicYear = null;

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());
        }

        return new Holiday(param.getId(), param.getSchoolId(), param.getName(), param.getDate(), param.getKind(), academicYear,
                param.isFixed(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static HolidayEntity param(Holiday param) {
        AcademicYearEntity academicYear = null;

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toEntity(param.getAcademicYear());
        }

        return HolidayEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .kind(param.getKind())
                .date(param.getDate())
                .academicYear(academicYear)
                .fixed(param.isFixed())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
