package com.moriba.skultem.application.dto;

import java.time.Instant;
import com.moriba.skultem.domain.model.House.Status;

public record HouseDTO(String id, String schoolId, String name, String motto, String color, TeacherDTO houseMaster,
                Status status, Instant createdAt, Instant updatedAt) {
}
