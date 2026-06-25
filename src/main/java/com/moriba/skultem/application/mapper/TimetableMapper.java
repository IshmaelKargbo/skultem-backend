package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TimetableDTO;
import com.moriba.skultem.domain.model.Timetable;

public class TimetableMapper {
    public static TimetableDTO toDTO(Timetable param) {
        if (param == null)
            return null;

        var subject = param.getTeacherSubject().getSubject();
        var teacher = param.getTeacherSubject().getTeacher();

        return new TimetableDTO(param.getId(), subject.getName(), param.getTeacherSubject().getId(), teacher.getName(),
                param.getRoom().getName(), param.getRoom().getId(), param.getColor(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
