package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

@Repository
public interface StudentJpaRepository extends JpaRepository<StudentEntity, String> {
    boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId);

    @Query("""
                SELECT s
                FROM StudentEntity s
                WHERE s.schoolId = :schoolId
                  AND (
                        LOWER(s.givenNames) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(s.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                ORDER BY s.createdAt DESC
            """)
    Page<StudentEntity> search(
            @Param("schoolId") String schoolId,
            @Param("search") String search,
            Pageable pageable);

    Page<StudentEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Page<StudentEntity> findAllBySchoolIdAndParent_IdOrderByCreatedAtDesc(String schoolId, String parentId,
            Pageable pageable);

    Optional<StudentEntity> findByIdAndSchoolId(String id, String schoolId);
}
