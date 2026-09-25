package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.vo.Level;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// The whole school structure in one payload - see UpdateSchoolStructureUseCase for the rules.
// sections is ignored for a UNIFIED school.
public record SchoolStructureRequestDTO(
        @NotNull(message = "Choose how the school is managed") ManagementModel managementModel,

        @NotEmpty(message = "Select at least one school level") List<Level> levels,

        @Valid List<Section> sections) {

    // id is null for a new section, the existing id when editing one.
    public record Section(String id, String name, List<Level> levels) {
    }
}
