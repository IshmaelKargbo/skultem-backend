package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.vo.Role;

public record UserDTO(String id, String name, String givenNames, String familyName, String email, String status,
                // schoolStatus is the SchoolUser membership status *at the school the request is
                // scoped to* - distinct from status above (the account itself, shared across every
                // school it belongs to). Null wherever the caller hasn't resolved a school context.
                String schoolStatus,
                String photo, List<Role> roles, Instant createdAt, Instant updatedAt) {

}
