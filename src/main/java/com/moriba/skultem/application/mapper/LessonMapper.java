package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.LessonDTO;
import com.moriba.skultem.domain.model.Lesson;

public class LessonMapper {

    public static LessonDTO toDTO(Lesson param) {
        if (param == null)
            return null;

        return new LessonDTO(
                param.getId(),
                param.getWeek().getId(),
                param.getWeek().getTopic(),
                param.getWeek().getSubTopic(),
                param.getTitle(),
                param.getContent(),
                param.getDate(),
                param.getDuration(),
                param.getObjectives(),
                param.getPreviousKnowledge(),
                param.getTeachingAids(),
                param.getReferenceMaterials(),
                param.getPresentation(),
                param.getEvaluation(),
                param.getAssignment(),
                param.getState().name(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
