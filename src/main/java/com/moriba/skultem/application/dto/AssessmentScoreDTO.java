package com.moriba.skultem.application.dto;

public record AssessmentScoreDTO(String id, String name, String assessment, Integer score, Integer weight,
        Integer weightScore, String status, String grade) {
}
