package com.moriba.skultem.application.dto;

// One outcome per uploaded CSV row (one row = one week), so the caller can show exactly which
// weeks were created, skipped (already existed), or failed and why - rather than an all-or-nothing
// result for a file that might have dozens of rows across many schemes.
public record BulkSchemeRowResultDTO(
        int row,
        String className,
        String subjectName,
        String termName,
        Integer week,
        String topic,
        String outcome, // CREATED, SKIPPED, FAILED
        String message) {
}
