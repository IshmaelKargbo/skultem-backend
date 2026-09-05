package com.moriba.skultem.application.dto;

public record TeacherClassMasterDTO(
        String classMasterId,
        String sessionId,
        String classId,
        // Null when the class has no streams (e.g. a class with a single, undivided
        // section) - the class detail page treats a missing stream as "show every
        // student in the class" rather than a specific stream's roster, so this must
        // stay null there rather than being coerced to an empty string.
        String streamId,
        String sessionName,
        String className,
        int studentCount,
        String promotionStatus) {
}
