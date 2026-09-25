package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.Role;

import jakarta.validation.constraints.NotNull;

// Empty sectionIds = whole school.
public record AssignStaffSectionsDTO(@NotNull(message = "Role is required") Role role, List<String> sectionIds) {
}
