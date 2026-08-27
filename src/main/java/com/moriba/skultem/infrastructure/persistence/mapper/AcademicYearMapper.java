package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;

public class AcademicYearMapper {
    public static AcademicYear toDomain(AcademicYearEntity param) {
        if (param == null) {
            return null;
        }

        AcademicYearEntity next = param.getNextYear();
        AcademicYear nextYear = next == null ? null
                : new AcademicYear(next.getId(), next.getSchoolId(), next.getName(), next.getStartDate(),
                        next.getEndDate(), next.getActive(), null, next.getStatus(), next.getCreatedAt(),
                        next.getUpdatedAt());

        return new AcademicYear(param.getId(), param.getSchoolId(), param.getName(), param.getStartDate(),
                param.getEndDate(), param.getActive(), nextYear, param.getStatus(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static AcademicYearEntity toEntity(AcademicYear args) {
        if (args == null) {
            return null;
        }

        var nextYear = args.getNextYear();

        return AcademicYearEntity.builder()
                .id(args.getId())
                .name(args.getName())
                .startDate(args.getStartDate())
                .endDate(args.getEndDate())
                .active(args.isActive())
                .status(args.getStatus())
                .schoolId(args.getSchoolId())
                .nextYear(nextYear == null ? null : AcademicYearEntity.builder().id(nextYear.getId()).build())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
