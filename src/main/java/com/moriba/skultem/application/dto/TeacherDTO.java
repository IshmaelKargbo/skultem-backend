package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Address;

public record TeacherDTO(String id, String schoolId, String phone,
        String staffId, UserDTO user, Address address, String status, Instant createdAt, Instant updatedAt) {
}
