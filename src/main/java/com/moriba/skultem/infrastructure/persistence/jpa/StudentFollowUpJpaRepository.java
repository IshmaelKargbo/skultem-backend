package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.StudentFollowUpEntity;

public interface StudentFollowUpJpaRepository extends JpaRepository<StudentFollowUpEntity, String> {
    List<StudentFollowUpEntity> findAllBySchoolIdAndClassIdOrderByCreatedAtDesc(String schoolId, String classId);
}
