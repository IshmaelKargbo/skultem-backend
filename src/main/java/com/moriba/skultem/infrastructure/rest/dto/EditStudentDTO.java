package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EditStudentDTO(
                @NotBlank(message = "Admission number is required") @Size(min = 1, max = 50, message = "Admission number must be between 1 and 50 characters") String admissionNumber,

                @NotBlank(message = "Given names are required") @Size(min = 2, max = 100, message = "Given names must be between 2 and 100 characters") String givenNames,

                @NotBlank(message = "Family name is required") @Size(min = 2, max = 100, message = "Family name must be between 2 and 100 characters") String familyName,

                @NotBlank(message = "Gender is required") @Pattern(regexp = "MALE|FEMALE", message = "Gender must be MALE or FEMALE") String gender,

                @NotNull(message = "Date of birth is required") @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,

                @NotBlank(message = "Nationality is required") String nationality,

                @NotBlank(message = "Religion is required") String religion,

                @NotBlank(message = "City is required") @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters") String city,

                @NotBlank(message = "Street is required") @Size(min = 5, max = 255, message = "Street must be between 5 and 255 characters") String street

) {
}
