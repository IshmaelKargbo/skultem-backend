package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

// One row of the Term Summary's class-level breakdown. Scoped to one class SESSION (class +
// section + stream), not the whole class - "SSS 1 Science" and "SSS 1 Art" are different rows,
// same boundary the attendance term/class summaries already respect.
public record ClassFeeSummaryRowDTO(
        String classSessionId,
        String className,
        String sectionName,
        String streamName,
        BigDecimal expected,
        BigDecimal collected,
        BigDecimal outstanding,
        double collectionRate,
        int totalStudents,
        int fullyPaidStudents,
        int studentsWithBalance) {
}
