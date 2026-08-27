package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClassMasterRecord(String id, ClassSessionRecord session, TeacherDTO teacher, Instant assignedAt, Instant endedAt,
        Instant createdAt, Instant updatedAt) {

}
