package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.MaterialCategoryEntity;

public interface MaterialCategoryJpaRepository extends JpaRepository<MaterialCategoryEntity, String> {
    Page<MaterialCategoryEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<MaterialCategoryEntity> findByIdAndSchoolId(String id, String schoolId);

    // Matches on name or description - backs the material category list's search box.
    @Query("""
                select c from MaterialCategoryEntity c
                where c.schoolId = :schoolId
                and (
                    lower(c.name) like lower(concat('%', :query, '%'))
                    or lower(c.description) like lower(concat('%', :query, '%'))
                )
                order by c.createdAt desc
            """)
    Page<MaterialCategoryEntity> search(@Param("schoolId") String schoolId, @Param("query") String query,
            Pageable pageable);
}
