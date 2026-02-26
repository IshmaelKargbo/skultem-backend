package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassStreamEntity;

@Repository
public interface ClassStreamJpaRepository extends JpaRepository<ClassStreamEntity, String> {
        boolean existsByClazz_IdAndSchoolIdAndStream_Id(String classId, String schoolId, String streamId);

        List<ClassStreamEntity> findAllByClazz_IdAndSchoolId(String classId, String schoolId);

        Optional<ClassStreamEntity> findByClazz_IdAndSchoolId(String classId, String schoolId);

        Optional<ClassStreamEntity> findByClazz_IdAndStream_IdAndSchoolId(String classId, String streamId, String schoolId);

        Optional<ClassStreamEntity> findByIdAndSchoolId(String id, String schoolId);

        long countBySchoolId(String schoolId);
}
