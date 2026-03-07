package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Parent;

public interface ParentRepository {
    void save(Parent domain);

    Optional<Parent> findById(String id);

    Optional<Parent> findByIdAndSchoolId(String id, String school);

    Optional<Parent> findByUserId(String userId);

    boolean existsByPhoneAndSchool(String phone, String schoolId);

    Page<Parent> findBySchool(String schoolId, Pageable pageable);

    long countAll();
}
