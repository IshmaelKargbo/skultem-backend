package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.domain.vo.LessonStage;

public record LessonDTO(
        String id,
        String weekId,
        String topic,
        String subTopic,
        String title,
        String content,
        LocalDate date,
        String duration,
        List<String> objectives,
        String previousKnowledge,
        List<String> teachingAids,
        List<String> referenceMaterials,
        List<LessonStage> presentation,
        String evaluation,
        String assignment,
        String state,
        Instant createdAt,
        Instant updatedAt) {
}
