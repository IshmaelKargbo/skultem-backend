package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.LibraryCategory;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryCategoryEntity;

public class LibraryCategoryMapper {
    public static LibraryCategory toDomain(LibraryCategoryEntity param) {
        if (param == null) return null;

        return new LibraryCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDescription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static LibraryCategoryEntity toEntity(LibraryCategory param) {
        if (param == null) return null;

        return LibraryCategoryEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
