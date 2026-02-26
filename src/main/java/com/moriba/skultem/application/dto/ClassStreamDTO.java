package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClassStreamDTO(String id, ClassDTO clazz, StreamDTO stream, Instant createdAt, Instant updatedAt) {

}
