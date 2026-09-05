package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import com.moriba.skultem.infrastructure.persistence.entity.SalaryTemplateEntity;

public interface SalaryTemplateJpaRepository extends JpaRepository<SalaryTemplateEntity, String> {
    Optional<SalaryTemplateEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<SalaryTemplateEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    void deleteByIdAndSchoolId(String id, String schoolId);

    @Query("""
                SELECT t FROM SalaryTemplateEntity t
                WHERE t.schoolId = :schoolId
                AND LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<SalaryTemplateEntity> search(@Param("schoolId") String schoolId, @Param("query") String query,
            Pageable pageable);
}
