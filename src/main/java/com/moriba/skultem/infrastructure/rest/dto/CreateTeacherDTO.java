package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeacherDTO(

                @NotBlank(message = "Given names are required") @Size(min = 2, max = 100, message = "Given names must be between 2 and 100 characters") String givenNames,

                @NotBlank(message = "Family name is required") @Size(min = 2, max = 100, message = "Family name must be between 2 and 100 characters") String familyName,

                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 150, message = "Email must not exceed 150 characters") String email,

                @NotBlank(message = "Phone is required") @Pattern(regexp = "^[0-9+ ]{7,20}$", message = "Phone number must contain only digits, spaces or + and be between 7 and 20 characters") String phone,

                @NotBlank(message = "Staff id is required") @Size(min = 5, max = 255, message = "Staff id must be between 5 and 255 characters") String staffId,

                @NotBlank(message = "Gender is required") @Pattern(regexp = "MALE|FEMALE", message = "Gender must be MALE or FEMALE") String gender,

                @NotBlank(message = "Title is required") @Pattern(regexp = "MR|MRS|MISS|MS|DR|PROF|REV|ENG", message = "Title must be MR, MRS, MISS, MS, DR, PROF, REV or ENG") String title,

                @NotBlank(message = "Street is required") @Size(min = 5, max = 255, message = "Street must be between 5 and 255 characters") String street,

                @NotBlank(message = "City is required") @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters") String city,

                String classMaster

) {
}
