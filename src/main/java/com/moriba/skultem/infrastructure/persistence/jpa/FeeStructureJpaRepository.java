package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;

@Repository
public interface FeeStructureJpaRepository extends JpaRepository<FeeStructureEntity, String> {
    boolean existsByAcademicYear_IdAndClazz_IdAndTerm_IdAndCategory_IdAndSchoolId(String academicYearId, String classId,
            String termId, String categoryId, String schoolId);

    Page<FeeStructureEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<FeeStructureEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<FeeStructureEntity> findAllByClazz_IdAndSchoolIdOrderByCreatedAtDesc(String classId, String schoolId, Pageable pageable);

    Page<FeeStructureEntity> findAllByAcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(String academic, String schoolId, Pageable pageable);

    Page<FeeStructureEntity> findAllByAcademicYear_IdAndClazz_IdAndSchoolIdOrderByCreatedAtDesc(String academicId, String classId, String schoolId, Pageable pageable);

    Page<FeeStructureEntity> findAllByTerm_IdAndSchoolIdOrderByCreatedAtDesc(String termId, String schoolId, Pageable pageable);
}
