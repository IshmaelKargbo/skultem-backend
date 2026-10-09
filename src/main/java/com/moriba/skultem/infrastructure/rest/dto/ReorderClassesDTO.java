package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

// Class ids in the order they should be ranked, first = lowest rank.
public record ReorderClassesDTO(@NotEmpty(message = "Classes are required") List<String> classIds) {
}
