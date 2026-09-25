package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ManagementSectionEntity;

public interface ManagementSectionJpaRepository extends JpaRepository<ManagementSectionEntity, String> {
    List<ManagementSectionEntity> findAllBySchoolIdOrderByDisplayOrderAsc(String schoolId);
}
