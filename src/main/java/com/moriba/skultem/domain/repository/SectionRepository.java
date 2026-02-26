package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Section;

public interface SectionRepository {
    void save(Section domain);

    Optional<Section> findById(String id);

    Optional<Section> findByIdAndSchoolId(String id, String schoolId);

    Optional<Section> findByNameAndSchoolId(String name, String schoolId);

    boolean existfindByNameAndSchoolId(String name, String schoolId);

    Page<Section> findBySchoolId(String schoolId, Pageable pageable);

    void delete(Section domain);
}
