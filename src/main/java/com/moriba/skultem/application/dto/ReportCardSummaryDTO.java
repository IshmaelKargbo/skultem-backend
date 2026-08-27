package com.moriba.skultem.application.dto;

import java.time.Instant;

// One row of the report-cards list page - deliberately thinner than
// ReportCardDTO (no subjects payload) since the list can hold hundreds of
// rows and the subject breakdown only matters once a single card is opened.
public record ReportCardSummaryDTO(String id, String studentId, String studentName, String admissionNumber,
        String photo, String className, String termName, String academicYearName, double average, int position,
        String overallGrade, boolean passed, int downloadCount, Instant generatedAt) {
}
