package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExpenseCategoryEntity;

public interface ExpenseCategoryJpaRepository extends JpaRepository<ExpenseCategoryEntity, String> {
    Page<ExpenseCategoryEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    // Name or description containing the words typed (case ignored).
    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM ExpenseCategoryEntity c
            WHERE c.schoolId = :schoolId
              AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.dscription, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY c.createdAt DESC
            """)
    Page<ExpenseCategoryEntity> search(@org.springframework.data.repository.query.Param("schoolId") String schoolId, @org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    boolean existsByNameIgnoreCaseAndSchoolIdAndIdNot(String name, String schoolId, String id);

    @org.springframework.data.jpa.repository.Query(value = "SELECT count(*) FROM expenses WHERE category_id = :categoryId AND school_id = :schoolId", nativeQuery = true)
    long countExpenses(@org.springframework.data.repository.query.Param("categoryId") String categoryId,
            @org.springframework.data.repository.query.Param("schoolId") String schoolId);

    Optional<ExpenseCategoryEntity> findByIdAndSchoolId(String id, String schoolId);
}
