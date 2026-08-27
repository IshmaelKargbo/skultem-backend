package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ReportCardSettingEntity;

public interface ReportCardSettingJpaRepository extends JpaRepository<ReportCardSettingEntity, String> {
    Optional<ReportCardSettingEntity> findBySchoolId(String schoolId);
}
