package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Material;

public interface MaterialRepository {
    void save(Material domain);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<Material> findByIdAndSchool(String id, String schoolId);

    Page<Material> findBySchool(String schoolId, Pageable pageable);
}
