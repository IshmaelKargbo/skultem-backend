package com.moriba.skultem.application.dto;

// One bucket of the religion breakdown - label is one of the school's canonical categories
// (Muslim/Christian/Other/Not specified), not the raw free-text value students were entered with.
public record ReligionCountDTO(
        String label,
        long count) {
}
