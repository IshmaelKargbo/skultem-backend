package com.moriba.skultem.application.dto;

public record TeacherClassMasterDTO(
        String classMasterId,
        String sessionId,
        String classId,
        String sessionName,
        String className,
        int studentCount,
        String promotionStatus) {
}
