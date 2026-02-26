package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;

public class StreamMapper {
    public static Stream toDomain(StreamEntity param) {
        return new Stream(param.getId(), param.getName(), param.getSchoolId(), param.getDescription(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static StreamEntity toEntity(Stream param) {
        return StreamEntity.builder()
                .id(param.getId())
                .name(param.getName())
                .schoolId(param.getSchoolId())
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
