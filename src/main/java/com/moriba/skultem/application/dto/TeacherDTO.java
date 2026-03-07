package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Gender;
import com.moriba.skultem.domain.model.vo.Title;

public record TeacherDTO(String id, String schoolId, String phone, Gender gender, Title title,
                String staffId, UserDTO user, String street, String city, String status, Instant createdAt,
                Instant updatedAt) {
}
