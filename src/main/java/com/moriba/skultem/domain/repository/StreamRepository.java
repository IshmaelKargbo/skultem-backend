package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Stream;

public interface StreamRepository {
    void save(Stream domain);

    Optional<Stream> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByNameAndSchool(String name, String schoolId);

    Page<Stream> findBySchool(String schoolId, Pageable pageable);
}
