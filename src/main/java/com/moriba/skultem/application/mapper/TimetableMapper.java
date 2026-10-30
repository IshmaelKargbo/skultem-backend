package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TimetableDTO;
import com.moriba.skultem.domain.model.Timetable;

public class TimetableMapper {
    public static TimetableDTO toDTO(Timetable param) {
        if (param == null)
            return null;

        var room = param.getRoom();
        if (param.isLibrary()) {
            return new TimetableDTO(param.getId(), param.label(), null, null, room == null ? null : room.getName(),
                    room == null ? null : room.getId(), param.getColor(), param.getCreatedAt(), param.getUpdatedAt(),
                    param.getKind().name());
        }

        var subject = param.getTeacherSubject().getSubject();
        var teacher = param.getTeacherSubject().getTeacher();

        return new TimetableDTO(param.getId(), subject.getName(), param.getTeacherSubject().getId(), teacher.getName(),
                room.getName(), room.getId(), param.getColor(), param.getCreatedAt(),
                param.getUpdatedAt(), param.getKind().name());
    }
}
