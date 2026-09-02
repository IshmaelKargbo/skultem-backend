package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AttendanceLocationSettingEntity;

public interface AttendanceLocationSettingJpaRepository extends JpaRepository<AttendanceLocationSettingEntity, String> {
    Optional<AttendanceLocationSettingEntity> findBySchoolId(String schoolId);
}
