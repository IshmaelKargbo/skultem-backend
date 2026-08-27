package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.domain.vo.LessonStage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLessonDTO(

        @NotBlank(message = "Week is required")
        String week,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        @NotNull(message = "Date is required")
        LocalDate date,

        String duration,

        List<String> objectives,

        String previousKnowledge,

        List<String> teachingAids,

        List<String> referenceMaterials,

        List<LessonStage> presentation,

        String evaluation,

        String assignment

) {}
