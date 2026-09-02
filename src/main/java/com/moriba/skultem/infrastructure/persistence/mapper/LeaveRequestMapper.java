package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.LeaveRequest;
import com.moriba.skultem.infrastructure.persistence.entity.LeaveRequestEntity;

public class LeaveRequestMapper {
    public static LeaveRequest toDomain(LeaveRequestEntity param) {
        if (param == null) {
            return null;
        }

        return new LeaveRequest(
                param.getId(),
                param.getSchoolId(),
                TeacherMapper.toDomain(param.getTeacher()),
                param.getType(),
                param.getStartDate(),
                param.getEndDate(),
                param.getReason(),
                param.getStatus(),
                param.getReviewNote(),
                param.getReviewedAt(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static LeaveRequestEntity toEntity(LeaveRequest param) {
        if (param == null) {
            return null;
        }

        return LeaveRequestEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .teacher(TeacherMapper.toEntity(param.getTeacher()))
                .type(param.getType())
                .startDate(param.getStartDate())
                .endDate(param.getEndDate())
                .reason(param.getReason())
                .status(param.getStatus())
                .reviewNote(param.getReviewNote())
                .reviewedAt(param.getReviewedAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
