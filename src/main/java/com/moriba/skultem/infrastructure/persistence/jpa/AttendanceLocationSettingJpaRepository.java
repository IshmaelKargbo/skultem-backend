package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AttendanceLocationSettingEntity;

public interface AttendanceLocationSettingJpaRepository extends JpaRepository<AttendanceLocationSettingEntity, String> {
    Optional<AttendanceLocationSettingEntity> findBySchoolIdAndManagementSectionIdIsNull(String schoolId);

    Optional<AttendanceLocationSettingEntity> findBySchoolIdAndManagementSectionId(String schoolId,
            String managementSectionId);

    List<AttendanceLocationSettingEntity> findAllBySchoolId(String schoolId);
}
