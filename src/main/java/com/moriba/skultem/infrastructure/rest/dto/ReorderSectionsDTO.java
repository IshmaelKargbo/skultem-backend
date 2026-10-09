package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

// Section ids in the order they should be ranked, first = lowest rank.
public record ReorderSectionsDTO(@NotEmpty(message = "Sections are required") List<String> sectionIds) {
}
