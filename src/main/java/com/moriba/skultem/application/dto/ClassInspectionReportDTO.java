package com.moriba.skultem.application.dto;

// One class session's inspection report within an "All classes" inspection report - the same
// InspectionReportDTO that class gets on its own, plus the names needed to title its block.
// streamName is null when the class has no stream.
public record ClassInspectionReportDTO(
        String classSessionId,
        String className,
        String sectionName,
        String streamName,
        InspectionReportDTO report) {
}
