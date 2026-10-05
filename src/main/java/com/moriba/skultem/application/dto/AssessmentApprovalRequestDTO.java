package com.moriba.skultem.application.dto;

import java.util.List;

public record AssessmentApprovalRequestDTO(String id, String teacher, String subject, String assessment, String term, String clazz,
        long studentCount, long pass, double passPercentage, long fail, double failPercentage, long avg, double avgPercentage, double avergeScore, String note, String status, List<AssessmentScoreDTO> studentScores,
        String teacherSubjectId, String assessmentId, String termId,
        // The class master taught this subject themselves, so an admin/proprietor/owner reviews it instead.
        boolean requiresAdminReview,
        // CLASS_MASTER or ADMIN - who approves grades for this class (see GradeApprovalResolver).
        String approver) {
}
