package com.moriba.skultem.application.dto;

import java.util.List;

// SearchUsersAcrossSchoolsUseCase's result shape - a user plus every school they belong to,
// unlike UserDTO/ListUserBySchoolUseCase which are always scoped to one school already.
public record UserWithSchoolsDTO(String id, String givenNames, String familyName, String email, String photo,
        List<UserSchoolMembershipDTO> schools) {
}
