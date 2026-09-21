package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateClassDTO(
                @NotBlank(message = "Name is required") String name,

                @NotBlank(message = "Level is required") String level,

                // Optional - the class's position in lists. Left out, it goes after the school's last class
                // (see CreateClassUseCase); the UI no longer asks for it.
                Integer levelOrder,

                @NotNull(message = "Sections are required") @NotEmpty(message = "At least one section is required") List<@NotBlank(message = "Section session cannot be blank") String> sections,
                List<String> streams,
                String assessmentTemplateId) {
}
