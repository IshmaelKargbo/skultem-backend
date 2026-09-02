package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.LeaveRequestDTO;
import com.moriba.skultem.domain.model.LeaveRequest;

public class LeaveRequestMapper {
    public static LeaveRequestDTO toDTO(LeaveRequest param) {
        if (param == null) {
            return null;
        }

        return new LeaveRequestDTO(
                param.getId(),
                param.getSchoolId(),
                TeacherMapper.toDTO(param.getTeacher()),
                param.getType(),
                param.getStartDate(),
                param.getEndDate(),
                param.durationDays(),
                param.getReason(),
                param.getStatus(),
                param.getReviewNote(),
                param.getReviewedAt(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
