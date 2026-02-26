package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public record CreateStudentDTO(
                @NotBlank(message = "Given names is required") String givenNames,

                @NotBlank(message = "Family name is required") String familyName,

                @NotBlank(message = "Academic number is required") String academicNumber,

                @NotBlank(message = "Gender is required") @Pattern(regexp = "MALE|FEMALE", message = "Gender must be MALE or FEMALE") String gender,

                @NotNull(message = "Date of birth is required") @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth) {
}
