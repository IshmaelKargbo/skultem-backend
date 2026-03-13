package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ReportConfig;
import com.moriba.skultem.infrastructure.persistence.entity.ReportConfigEntity;

public class ReportConfigMapper {
    public static ReportConfig toDomain(ReportConfigEntity param) {
        if (param == null) {
            return null;
        }

        return new ReportConfig(
                param.getId(),
                param.getSchoolId(),
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

    public static ReportConfigEntity toEntity(ReportConfig param) {
        if (param == null) {
            return null;
        }

        return ReportConfigEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .type(param.getType())
                .format(param.getFormat())
                .classId(param.getClassId())
                .classSessionId(param.getClassSessionId())
                .teacherSubjectId(param.getTeacherSubjectId())
                .termId(param.getTermId())
                .startDate(param.getStartDate())
                .endDate(param.getEndDate())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
