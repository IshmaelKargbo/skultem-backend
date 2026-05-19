package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.infrastructure.rest.dto.validation.ValidParentOrParentId;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

@ValidParentOrParentId
public record CreateStudentDTO(

        @NotBlank(message = "Given names is required")
        String givenNames,

        @NotBlank(message = "Family name is required")
        String familyName,

        @NotBlank(message = "Nationality is required")
        String nationality,

        @NotBlank(message = "Religion is required")
        String religion,

        @NotBlank(message = "Enrollment type is required")
        @Pattern(
            regexp = "NEW|TRANSFER|RE_ENROLLMENT",
            message = "Enrollment type is invalid"
        )
        String enrollmentType,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Address is required")
        String street,

        @NotBlank(message = "Class is required")
        String classId,

        String previousSchool,

        String house,

        String lastClass,

        String parentId,

        CreateParentDTO parent,

        FamilyDTO family,

        @NotBlank(message = "Admission number is required")
        String admissionNumber,

        @NotNull(message = "Admission date is required")
        LocalDate admissionDate,

        @NotBlank(message = "Gender is required")
        @Pattern(
            regexp = "MALE|FEMALE",
            message = "Gender must be MALE or FEMALE"
        )
        String gender,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        List<String> selectedOptionIds

) {
}