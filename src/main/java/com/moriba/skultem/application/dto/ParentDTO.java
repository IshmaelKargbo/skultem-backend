package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ParentDTO(String id, String schoolId, String phone, String name, String givenNames, String familyName,
                String email, String street, String city, String fatherName, String motherName, String status,
                FeeDetail feeDetail, Integer students, Instant createdAt, Instant updatedAt) {
}
