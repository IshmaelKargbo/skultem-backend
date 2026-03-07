package com.moriba.skultem.application.dto;

import java.time.Instant;

public record UserDTO(String id, String name, String givenNames, String familyName, String email, String status,
        String school, String role, Instant createdAt, Instant updatedAt) {

}
