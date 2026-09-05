package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moriba.skultem.infrastructure.persistence.entity.SchoolEntity;

public interface SchoolJpaRepository extends JpaRepository<SchoolEntity, String> {
    boolean existsByDomainIgnoreCase(String domain);

    Optional<SchoolEntity> findByDomainIgnoreCase(String domain);

    @Query("""
                select s from SchoolEntity s
                where lower(s.name) like lower(concat('%', :query, '%'))
                   or lower(s.domain) like lower(concat('%', :query, '%'))
            """)
    Page<SchoolEntity> search(String query, Pageable pageable);
}