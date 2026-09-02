package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.SalaryStructureEntity;

public interface SalaryStructureJpaRepository extends JpaRepository<SalaryStructureEntity, String> {
    Optional<SalaryStructureEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<SalaryStructureEntity> findByTeacher_IdAndSchoolId(String teacherId, String schoolId);

    boolean existsByTeacher_IdAndSchoolId(String teacherId, String schoolId);

    List<SalaryStructureEntity> findAllBySchoolId(String schoolId);

    @Query("""
                SELECT s FROM SalaryStructureEntity s
                JOIN s.teacher t
                JOIN t.user u
                WHERE s.schoolId = :schoolId
                AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(t.staffId) LIKE LOWER(CONCAT('%', :search, '%'))
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SalaryStructureEntity> search(@Param("schoolId") String schoolId, @Param("search") String search,
            Pageable pageable);
}
