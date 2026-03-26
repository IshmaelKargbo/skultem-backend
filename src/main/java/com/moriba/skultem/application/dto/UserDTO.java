package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.vo.Role;

public record UserDTO(String id, String name, String givenNames, String familyName, String email, String status,
                List<Role> roles, Instant createdAt, Instant updatedAt) {

}
