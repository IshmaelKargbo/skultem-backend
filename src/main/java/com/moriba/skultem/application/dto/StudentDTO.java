package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Student.Gender;
import com.moriba.skultem.domain.model.Student.Status;

public record StudentDTO(String id, String admissionNumber, String givenNames, String familyName, Gender gender,
        LocalDate dateOfBirth, Status status, Instant createdAt, Instant updatedAt) {
}
