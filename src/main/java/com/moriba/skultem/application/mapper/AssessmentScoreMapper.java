package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.domain.model.AssessmentScore;

public class AssessmentScoreMapper {

    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade) {
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
                Double.NaN,
                param.getCycle().getAssessment().getPosition(),
                param.getStatus().name(),
                grade,
                "");
    }

    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade, String trend, Double avarage) {
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
                avarage,
                param.getCycle().getAssessment().getPosition(),
                param.getStatus().name(),
                grade,
                trend);
    }
}
