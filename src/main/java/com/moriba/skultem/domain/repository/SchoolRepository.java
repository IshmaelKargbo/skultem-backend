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

    // Unscoped by tenant, unlike everything else here - only SystemAdminController's schools
    // list needs to look across every school at once. Matches on name/domain.
    Page<School> search(String query, Pageable pageable);

    Optional<School> findByDomain(String domain);

    void delete(School domain);

    long countAll();
}
