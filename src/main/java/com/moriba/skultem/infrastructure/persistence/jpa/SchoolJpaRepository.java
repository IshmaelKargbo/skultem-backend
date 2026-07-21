package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.SchoolEntity;

public interface SchoolJpaRepository extends JpaRepository<SchoolEntity, String> {
    boolean existsByDomainIgnoreCase(String domain);

    Optional<SchoolEntity> findByDomainIgnoreCase(String domain);
}