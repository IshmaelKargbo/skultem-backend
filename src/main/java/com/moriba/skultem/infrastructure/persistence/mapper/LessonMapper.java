package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.moriba.skultem.domain.model.Lesson;
import com.moriba.skultem.domain.vo.LessonStage;
import com.moriba.skultem.infrastructure.persistence.entity.LessonEntity;

public class LessonMapper {

    public static Lesson toDomain(LessonEntity param) {
        var week = WeekMapper.toDomain(param.getWeek());
        List<String> objectives = JsonMapper.fromJsonStringList(param.getObjectives());
        List<String> teachingAids = JsonMapper.fromJsonStringList(param.getTeachingAids());
        List<String> referenceMaterials = JsonMapper.fromJsonStringList(param.getReferenceMaterials());
        List<LessonStage> presentation = JsonMapper.fromJsonList(param.getPresentation(), LessonStage.class);

        return new Lesson(
                param.getId(),
                param.getSchoolId(),
                week,
                param.getTitle(),
                param.getLesson(),
                param.getDate(),
                param.getDuration(),
                objectives,
                param.getPreviousKnowledge(),
                teachingAids,
                referenceMaterials,
                presentation,
                param.getEvaluation(),
                param.getAssignment(),
                param.getState(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static LessonEntity toEntity(Lesson param) {
        return LessonEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .week(WeekMapper.toEntity(param.getWeek()))
                .title(param.getTitle())
                .lesson(param.getContent())
                .date(param.getDate())
                .duration(param.getDuration())
                .objectives(JsonMapper.toJson(param.getObjectives()))
                .previousKnowledge(param.getPreviousKnowledge())
                .teachingAids(JsonMapper.toJson(param.getTeachingAids()))
                .referenceMaterials(JsonMapper.toJson(param.getReferenceMaterials()))
                .presentation(JsonMapper.toJson(param.getPresentation()))
                .evaluation(param.getEvaluation())
                .assignment(param.getAssignment())
                .state(param.getState())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
