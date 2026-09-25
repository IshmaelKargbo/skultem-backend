package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.vo.Role;

public interface StaffManagementSectionRepository {
    List<StaffManagementSection> findBySchoolAndUserAndRole(String schoolId, String userId, Role role);

    List<StaffManagementSection> findBySchoolId(String schoolId);

    boolean existsBySectionId(String managementSectionId);

    // Replaces the whole set for (school, user, role); an empty list makes them whole-school again.
    void replace(String schoolId, String userId, Role role, List<String> managementSectionIds);
}
