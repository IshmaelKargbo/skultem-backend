package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.RequestDemo;
import com.moriba.skultem.infrastructure.persistence.entity.RequestDemoEntity;

public class RequestDemoMapper {
    public static RequestDemo toDomain(RequestDemoEntity param) {
        return new RequestDemo(param.getId(), param.getName(), param.getEmail(), param.getSchool(), param.getPhone(),
                param.getCity(), param.getAddress(), param.getPreferred(), param.getPriority(), param.getMessage(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static RequestDemoEntity toEntity(RequestDemo param) {
        return RequestDemoEntity.builder()
                .id(param.getId())
                .name(param.getName())
                .email(param.getEmail())
                .phone(param.getPhone())
                .address(param.getAddress())
                .city(param.getCity())
                .preferred(param.getPreferred())
                .priority(param.getPriority())
                .message(param.getMessage())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
