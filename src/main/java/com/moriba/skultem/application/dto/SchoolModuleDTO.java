package com.moriba.skultem.application.dto;

import java.util.List;

/**
 * One entry in a school's module catalog. {@code requires} / {@code requiredBy} are module keys, so
 * the Modules page can explain "needs Grading & Assessments" before an install and refuse a
 * disable that would break another installed module.
 */
public record SchoolModuleDTO(
        String key,
        String label,
        String description,
        String category,
        String categoryLabel,
        boolean installed,
        boolean starter,
        List<String> requires,
        List<String> requiredBy) {
}
