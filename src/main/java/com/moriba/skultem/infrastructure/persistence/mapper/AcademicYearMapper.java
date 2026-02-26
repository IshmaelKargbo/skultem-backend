package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;

public class AcademicYearMapper {
    public static AcademicYear toDomain(AcademicYearEntity param) {
        return new AcademicYear(param.getId(), param.getSchoolId(), param.getName(), param.getStartDate(),
                param.getEndDate(), param.getActive(), param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static AcademicYearEntity toEntity(AcademicYear args) {
        return AcademicYearEntity.builder()
                .id(args.getId())
                .name(args.getName())
                .startDate(args.getStartDate())
                .endDate(args.getEndDate())
                .active(args.isActive())
                .status(args.getStatus())
                .schoolId(args.getSchoolId())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
