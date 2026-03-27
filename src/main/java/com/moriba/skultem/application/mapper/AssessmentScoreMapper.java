package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.domain.model.AssessmentScore;

public class AssessmentScoreMapper {

    public static AssessmentScoreDTO toDTO(AssessmentScore param) {
        return new AssessmentScoreDTO(
                param.getId(),
                param.getAssessment().getName(),
                param.getAssessment().getId(),
                param.getCycle().getTerm().getName(),
                param.getStudentAssessment().getEnrollment().getStudent().getName(),
                param.getStudentAssessment().getTeacherSubject().getTeacher().getUser().getName(),
                param.getStudentAssessment().getTeacherSubject().getSubject().getName(),
                param.getStudentAssessment().getEnrollment().getClazz().getName(),
                param.getScore(),
                param.getWeight(),
                param.getWeightedScore(),
                param.getCycle().getAssessment().getPosition(),
                param.getStatus().name(),
                "",
                "");
    }

    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade, String trend) {
        int score = 0, weight = 0, weightedScore = 0;

        if (!grade.equals("N/A")) {
            score = param.getScore();
            weight = param.getWeight();
            weightedScore = param.getWeightedScore();
        }

        return new AssessmentScoreDTO(
                param.getId(),
                param.getAssessment().getName(),
                param.getAssessment().getId(),
                param.getCycle().getTerm().getName(),
                param.getStudentAssessment().getEnrollment().getStudent().getName(),
                param.getStudentAssessment().getTeacherSubject().getTeacher().getUser().getName(),
                param.getStudentAssessment().getTeacherSubject().getSubject().getName(),
                param.getStudentAssessment().getEnrollment().getClazz().getName(),
                score,
                weight,
                weightedScore,
                param.getCycle().getAssessment().getPosition(),
                param.getStatus().name(),
                grade,
                trend);
    }

    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade) {
        int score = 0, weight = 0, weightedScore = 0;

        if (!grade.equals("N/A")) {
            score = param.getScore();
            weight = param.getWeight();
            weightedScore = param.getWeightedScore();
        }

        return new AssessmentScoreDTO(
                param.getId(),
                param.getAssessment().getName(),
                param.getAssessment().getId(),
                param.getCycle().getTerm().getName(),
                param.getStudentAssessment().getEnrollment().getStudent().getName(),
                param.getStudentAssessment().getTeacherSubject().getTeacher().getUser().getName(),
                param.getStudentAssessment().getTeacherSubject().getSubject().getName(),
                param.getStudentAssessment().getEnrollment().getClazz().getName(),
                score,
                weight,
                weightedScore,
                param.getCycle().getAssessment().getPosition(),
                param.getStatus().name(),
                grade,
                "");
    }
}
