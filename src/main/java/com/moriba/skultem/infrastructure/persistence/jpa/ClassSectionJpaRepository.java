package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassSectionEntity;

@Repository
public interface ClassSectionJpaRepository extends JpaRepository<ClassSectionEntity, String> {
    boolean existsByClazz_IdAndSchoolId(String classId, String schoolId);

    boolean existsByClazz_IdAndSchoolIdAndSection_Id(String classId, String schoolId, String sectionId);

    Page<ClassSectionEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<ClassSectionEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<ClassSectionEntity> findByIdAndClazz_IdAndSchoolId(String id, String classId, String schoolId);

    List<ClassSectionEntity> findAllByClazz_IdAndSchoolId(String classId, String schoolId);
}
