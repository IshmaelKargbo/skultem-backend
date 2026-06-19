package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;

public record TeacherDTO(String id, String schoolId, String phone, Gender gender, Title title, List<String> classes,
                String staffId, UserDTO user, String street, String city, String status, Instant createdAt,
                Instant updatedAt) {
}
