package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.persistence.entity.StaffManagementSectionEntity;

public interface StaffManagementSectionJpaRepository extends JpaRepository<StaffManagementSectionEntity, String> {
    List<StaffManagementSectionEntity> findAllBySchoolIdAndUserIdAndRole(String schoolId, String userId, Role role);

    List<StaffManagementSectionEntity> findAllBySchoolId(String schoolId);

    boolean existsByManagementSectionId(String managementSectionId);

    // A bulk delete that runs immediately - a derived deleteBy would be queued behind the re-inserts
    // at flush time and trip the unique index when the same section is kept.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StaffManagementSectionEntity e where e.schoolId = :schoolId and e.userId = :userId and e.role = :role")
    void deleteAllBySchoolIdAndUserIdAndRole(@Param("schoolId") String schoolId, @Param("userId") String userId,
            @Param("role") Role role);
}
