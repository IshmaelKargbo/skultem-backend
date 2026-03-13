package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.TeacherEntity;

@Repository
public interface TeacherJpaRepository extends JpaRepository<TeacherEntity, String> {
    boolean existsByStaffIdAndSchoolId(String staffId, String schoolId);

    boolean existsByPhoneAndSchoolId(String staffId, String schoolId);

    Optional<TeacherEntity> findByUserId(String userId);

    Optional<TeacherEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<TeacherEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    long countBySchoolId(String schoolId);
}
