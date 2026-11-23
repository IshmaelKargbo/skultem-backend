package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.ExpenseEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface ExpenseJpaRepository extends JpaRepository<ExpenseEntity, String>, JpaSpecificationExecutor<ExpenseEntity> {
    Page<ExpenseEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    // Title, description or category name containing the words typed (case ignored).
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT e FROM ExpenseEntity e
            WHERE e.schoolId = :schoolId
              AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(e.category.name) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY e.createdAt DESC
            """, countQuery = """
            SELECT count(e) FROM ExpenseEntity e
            WHERE e.schoolId = :schoolId
              AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(e.category.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<ExpenseEntity> search(@org.springframework.data.repository.query.Param("schoolId") String schoolId, @org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    Optional<ExpenseEntity> findByIdAndSchoolId(String id, String schoolId);

    default Page<ExpenseEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<ExpenseEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }
}
