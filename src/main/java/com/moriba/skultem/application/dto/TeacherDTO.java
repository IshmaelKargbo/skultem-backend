package com.moriba.skultem.application.dto;

import java.time.Instant;

public record TeacherDTO(String id, String schoolId, String phone,
        String staffId, UserDTO user, String street, String city, String status, Instant createdAt, Instant updatedAt) {
}
