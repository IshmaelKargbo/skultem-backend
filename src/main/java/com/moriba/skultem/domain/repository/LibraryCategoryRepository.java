package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.LibraryCategory;

public interface LibraryCategoryRepository {
    void save(LibraryCategory domain);

    void deleteById(String id);

    boolean existsByNameAndSchool(String name, String schoolId);

    Optional<LibraryCategory> findByIdAndSchool(String id, String schoolId);

    // Matches on name or description; an empty query returns every category.
    Page<LibraryCategory> search(String schoolId, String query, Pageable pageable);

    long countBySchool(String schoolId);
}
