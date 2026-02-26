package com.moriba.skultem.application.dto;

import java.time.Instant;

public record SubjectGroupDTO(String id, String name, String level, String className, String classId, String streamName, String streamId, boolean required,
        int minSelection, int maxSelection, int displayOrder, Instant createdAt, Instant updatedAt) {
}
