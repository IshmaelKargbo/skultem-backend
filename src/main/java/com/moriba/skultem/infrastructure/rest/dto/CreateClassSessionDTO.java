package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record CreateClassSessionDTO(
                @NotNull(message = "Class id are required") String classId,
                @NotNull(message = "Academic year id are required") String academicYear,
                String streamId,
                @NotNull(message = "Section id are required") String sectionId
            ) {
}
