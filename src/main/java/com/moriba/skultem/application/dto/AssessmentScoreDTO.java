package com.moriba.skultem.application.dto;

public record AssessmentScoreDTO(String id, String name, String assessment, String term, String student, String teacher, String subject, String clazz,
                Integer score, Integer weight, Integer weightScore, Double avarage, Integer level, String status, String grade, String trend) {
}
