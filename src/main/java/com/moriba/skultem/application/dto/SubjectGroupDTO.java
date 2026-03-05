package com.moriba.skultem.application.dto;

import java.time.Instant;

public record SubjectGroupDTO(String id, String name, String className, String classId, String streamName,
                String streamId, int totalSelection, Instant createdAt, Instant updatedAt) {
}
