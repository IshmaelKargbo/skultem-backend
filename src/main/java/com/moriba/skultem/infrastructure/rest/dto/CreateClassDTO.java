package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateClassDTO(
                @NotBlank(message = "Name is required") String name,

                @NotBlank(message = "Level is required") String level,

                @NotNull(message = "Level order is required") Integer levelOrder,

                @NotNull(message = "Sections are required") @NotEmpty(message = "At least one section is required") List<@NotBlank(message = "Section name cannot be blank") String> sections,
                List<String> streams) {
}
