package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.SchoolLevelEntity;

public interface SchoolLevelJpaRepository extends JpaRepository<SchoolLevelEntity, String> {
    List<SchoolLevelEntity> findAllBySchoolId(String schoolId);
}
