package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.SubjectGroupEntity;

@Repository
public interface SubjectGroupJpaRepository extends JpaRepository<SubjectGroupEntity, String> {
    Page<SubjectGroupEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Page<SubjectGroupEntity> findAllByStream_IdAndSchoolIdOrderByClazz_LevelOrderAsc(String streamId, String schoolId, Pageable pageable);

    Page<SubjectGroupEntity> findAllByClazz_Id(String classId, Pageable pageable);

    Page<SubjectGroupEntity> findAllByClazz_IdAndSchoolIdOrderByClazz_LevelOrderAsc(String classId, String schoolId, Pageable pageable);

    Optional<SubjectGroupEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<SubjectGroupEntity> findByIdAndStream_IdAndSchoolId(String id, String streamId, String schoolid);

    Optional<SubjectGroupEntity> findByIdAndClazz_IdAndSchoolId(String id, String classId, String schoolid);

    List<SubjectGroupEntity> findAllByIdInAndSchoolId(Set<String> ids, String schoolId);

    List<SubjectGroupEntity> findAllByIdInAndStream_IdAndSchoolId(Set<String> ids, String streamId, String schoolId);
}
