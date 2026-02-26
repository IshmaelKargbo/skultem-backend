package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClassMasterDTO(String id, ClassSessionDTO session, TeacherDTO teacher, Instant assignedAt, Instant endedAt,
        Instant createdAt, Instant updatedAt) {

}
