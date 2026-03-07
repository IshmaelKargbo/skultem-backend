package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.School;


public interface SchoolRepository {
    void save(School domain);

    Optional<School> findById(String id);

    boolean existsByDomain(String domain);

    Page<School> findAll(Pageable pageable);

    Optional<School> findByDomain(String domain);

    void delete(School domain);

    long countAll();
}
