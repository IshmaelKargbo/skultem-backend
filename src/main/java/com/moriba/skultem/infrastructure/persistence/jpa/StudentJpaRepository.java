package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

@Repository
public interface StudentJpaRepository extends JpaRepository<StudentEntity, String> {
    boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId);

    Page<StudentEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<StudentEntity> findByIdAndSchoolId(String id, String schoolId);
}
