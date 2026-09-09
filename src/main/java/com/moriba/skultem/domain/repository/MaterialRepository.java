package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Material;

public interface MaterialRepository {
    void save(Material domain);

    void deleteById(String id);

    boolean existByNameAndSchoolId(String name, String schoolId);

    // Guards MaterialCategory deletion - a category still assigned to a material can't be removed.
    boolean existsByCategoryAndSchool(String categoryId, String schoolId);

    Optional<Material> findByIdAndSchool(String id, String schoolId);

    Page<Material> findBySchool(String schoolId, Pageable pageable);

    // Matches on material name or category name - backs the materials list's search box.
    Page<Material> search(String schoolId, String query, Pageable pageable);
}
