package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.infrastructure.persistence.entity.TimetableEntity;

public class TimetableMapper {
    public static Timetable toDomain(TimetableEntity param) {
        var period = PeriodMapper.toDomain(param.getPeriod());
        var day = WorkingDayMapper.toDomain(param.getDay());
        var room = RoomMapper.toDomain(param.getRoom());
        var teacherSubject = TeacherSubjectMapper.toDomain(param.getTeacherSubject());

        return new Timetable(param.getId(), param.getSchoolId(), period, teacherSubject, day, room, param.getColor(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static TimetableEntity toEntity(Timetable param) {
        return TimetableEntity.builder()
                .id(param.getId())
                .color(param.getColor())
                .day(WorkingDayMapper.toEntity(param.getDay()))
                .period(PeriodMapper.toEntity(param.getPeriod()))
                .teacherSubject(TeacherSubjectMapper.toEntity(param.getTeacherSubject()))
                .room(RoomMapper.toEntity(param.getRoom()))
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
