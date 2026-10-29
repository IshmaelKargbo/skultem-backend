package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.LibraryBook;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryBookEntity;

public class LibraryBookMapper {
    public static LibraryBook toDomain(LibraryBookEntity param) {
        if (param == null) return null;

        return new LibraryBook(param.getId(), param.getSchoolId(),
                LibraryCategoryMapper.toDomain(param.getCategory()), param.getTitle(), param.getAuthor(),
                param.getIsbn(), param.getPublisher(), param.getShelf(), param.getTotalCopies(),
                param.getAvailableCopies(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static LibraryBookEntity toEntity(LibraryBook param) {
        if (param == null) return null;

        return LibraryBookEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .category(LibraryCategoryMapper.toEntity(param.getCategory()))
                .title(param.getTitle())
                .author(param.getAuthor())
                .isbn(param.getIsbn())
                .publisher(param.getPublisher())
                .shelf(param.getShelf())
                .totalCopies(param.getTotalCopies())
                .availableCopies(param.getAvailableCopies())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
