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
                "",
                param.isPassed());
    }

    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade, String trend) {
        int score = 0, weight = 0, weightedScore = 0;
        Boolean passed = null;

        if (param.isApproved()) {
            score = param.getScore();
            weight = param.getWeight();
            weightedScore = param.getWeightedScore();
            passed = param.isPassed();
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
                trend,
                passed);
    }

    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade) {
        int score = 0, weight = 0, weightedScore = 0;
        Boolean passed = null;

        if (param.isApproved() || param.isSubmited() || param.isCompleted()) {
            score = param.getScore();
            weight = param.getWeight();
            weightedScore = param.getWeightedScore();
            passed = param.isPassed();
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
                "",
                passed);
    }

    // The plain score plus, when this assessment was opened as continuous assessment, its frozen structure and
    // this student's recordings (recording i+1 at index i, null until recorded).
    public static AssessmentScoreDTO toDTO(AssessmentScore param,
            java.util.List<com.moriba.skultem.domain.model.AssessmentCaEntry> entries) {
        return withContinuous(toDTO(param), param, entries);
    }

    // Same, for the approval view - which also shows each student's grade.
    public static AssessmentScoreDTO toDTO(AssessmentScore param, String grade,
            java.util.List<com.moriba.skultem.domain.model.AssessmentCaEntry> entries) {
        return withContinuous(toDTO(param, grade), param, entries);
    }

    private static AssessmentScoreDTO withContinuous(AssessmentScoreDTO base, AssessmentScore param,
            java.util.List<com.moriba.skultem.domain.model.AssessmentCaEntry> entries) {
        var cycle = param.getCycle();
        if (!cycle.isContinuous()) {
            return base;
        }
        var recordings = new java.util.ArrayList<Integer>();
        for (int i = 0; i < cycle.getCaEntries(); i++) {
            recordings.add(null);
        }
        for (var e : entries) {
            if (e.getEntryNumber() >= 1 && e.getEntryNumber() <= recordings.size()) {
                recordings.set(e.getEntryNumber() - 1, e.getScore());
            }
        }
        var continuous = new AssessmentScoreDTO.Continuous(cycle.getStructure().name(), cycle.getCaPercentage(),
                cycle.getFormalPercentage(), cycle.getCaFrequency().name(), cycle.getCaFrequency().getUnit(),
                cycle.getCaEntries(), cycle.getConfigVersion(), param.getCaScore(), param.getFormalScore(),
                com.moriba.skultem.domain.service.ContinuousAssessmentCalculator.points(param.getCaScore(), cycle.getCaPercentage()),
                com.moriba.skultem.domain.service.ContinuousAssessmentCalculator.points(param.getFormalScore(), cycle.getFormalPercentage()),
                recordings, cycle.isCaSubmitted(), new java.util.ArrayList<>(cycle.getCaLockedWeeks()));
        return new AssessmentScoreDTO(base.id(), base.name(), base.assessment(), base.term(), base.student(),
                base.teacher(), base.subject(), base.clazz(), base.score(), base.weight(), base.weightScore(),
                base.level(), base.status(), base.grade(), base.trend(), base.passed(), continuous);
    }
}
