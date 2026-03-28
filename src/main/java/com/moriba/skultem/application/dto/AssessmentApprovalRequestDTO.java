package com.moriba.skultem.application.dto;

import java.util.List;

public record AssessmentApprovalRequestDTO(String id, String teacher, String subject, String assessment, String term, String clazz,
        long studentCount, long pass, double passPercentage, long fail, double failPercentage, long avg, double avgPercentage, double avergeScore, String note, String status, List<AssessmentScoreDTO> studentScores) {
}
