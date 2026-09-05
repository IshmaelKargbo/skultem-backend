package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.vo.Role;

// One school a user belongs to, for SearchUsersAcrossSchoolsUseCase's cross-tenant view - status
// is the SchoolUser membership's own status (ACTIVE/RESET_PASSWORD/INACTIVE), not the user's
// account-wide status in UserDTO.
public record UserSchoolMembershipDTO(String schoolId, String schoolName, String domain, Role role, String status) {
}
