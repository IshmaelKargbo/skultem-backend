package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectEntity;

@Repository
public interface ClassSubjectJpaRepository extends JpaRepository<ClassSubjectEntity, String> {
    boolean existsByClazz_IdAndSubject_Id(String classId, String subjectId);

    boolean existsByClazz_IdAndSubject_IdAndSchoolId(String classId, String subjectId, String schoolId);

    Optional<ClassSubjectEntity> findByClazz_IdAndSubject_IdAndSchoolId(String classId, String subjectId,
            String schoolId);
    
    Page<ClassSubjectEntity> findAllByClazz_IdAndSchoolId(String classId, String schoolId, Pageable pageable);

    Page<ClassSubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Page<ClassSubjectEntity> findAllByClazz_Id(String classId, Pageable pageable);
}
