package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;

public class TermMapper {
    public static Term toDomain(TermEntity param) {
        AcademicYear academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());

        return new Term(param.getId(), param.getSchoolId(), param.getName(), param.getTermNumber(),
                param.getStartDate(), param.getEndDate(), academicYear, param.getStatus(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static TermEntity toEntity(Term args) {
        AcademicYearEntity academicYear = AcademicYearMapper.toEntity(args.getAcademicYear());
        return TermEntity.builder()
                .id(args.getId())
                .name(args.getName())
                .academicYear(academicYear)
                .startDate(args.getStartDate())
                .endDate(args.getEndDate())
                .termNumber(args.getTermNumber())
                .status(args.getStatus())
                .schoolId(args.getSchoolId())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
