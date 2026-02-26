package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.User;

public interface UserRepository {
    void save(User domain);

    Optional<User> findById(String id);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Page<User> findBySchool(String school, Pageable pageable);

    long countAll();

    void delete(User domain);
}
