package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AssignHouseDTO(
        @NotNull(message = "Student records are required") @NotEmpty(message = "Student records cannot be empty") List<AssignRecordDTO> records) {
}