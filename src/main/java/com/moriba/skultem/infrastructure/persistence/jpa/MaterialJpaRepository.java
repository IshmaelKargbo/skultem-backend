package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.MaterialEntity;

public interface MaterialJpaRepository extends JpaRepository<MaterialEntity, String> {
    Page<MaterialEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    boolean existsByCategory_IdAndSchoolId(String categoryId, String schoolId);

    Optional<MaterialEntity> findByIdAndSchoolId(String id, String schoolId);

    // Matches on material name or category name - backs the materials list's search box.
    @Query("""
                select m from MaterialEntity m
                join m.category c
                where m.schoolId = :schoolId
                and (
                    lower(m.name) like lower(concat('%', :query, '%'))
                    or lower(c.name) like lower(concat('%', :query, '%'))
                )
                order by m.createdAt desc
            """)
    Page<MaterialEntity> search(@Param("schoolId") String schoolId, @Param("query") String query, Pageable pageable);
}
