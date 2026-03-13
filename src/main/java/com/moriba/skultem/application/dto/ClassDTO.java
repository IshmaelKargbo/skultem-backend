package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.vo.Level;

public record ClassDTO(String id, String name, int levelOrder, ClassDTO nextClass, Level level,
                String assessmentTemplateId, String assessmentTemplateName, boolean terminal, Instant createdAt,
                Instant updatedAt) {

}
