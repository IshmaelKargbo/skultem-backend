package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.RequestDemo;

public interface RequestDemoRepository {
    void save(RequestDemo domain);

    Optional<RequestDemo> findById(String id);

    Page<RequestDemo> findAll(Pageable pageable);

    void delete(RequestDemo domain);

    long countAll();
}
