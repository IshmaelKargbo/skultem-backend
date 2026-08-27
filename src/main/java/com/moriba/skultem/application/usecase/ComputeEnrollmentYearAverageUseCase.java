package com.moriba.skultem.application.usecase;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ComputeEnrollmentYearAverageUseCase {

    private final AssessmentScoreRepository assessmentScoreRepo;

    public Double execute(String schoolId, String enrollmentId) {
        var scores = assessmentScoreRepo.findAllByEnrollmentIdAndSchoolId(enrollmentId, schoolId).stream()
                .filter(score -> score.getStatus() == ClassSubjectAssessmentLifeCycle.Status.APPROVED
                        || score.getStatus() == ClassSubjectAssessmentLifeCycle.Status.COMPLETED
                        || score.getStatus() == ClassSubjectAssessmentLifeCycle.Status.LOCKED)
                .toList();

        if (scores.isEmpty()) {
            return null;
        }

        Map<String, Double> totalBySubjectAndTerm = scores.stream()
                .collect(Collectors.groupingBy(
                        score -> score.getStudentAssessment().getTeacherSubject().getId() + "|"
                                + score.getStudentAssessment().getTerm().getId(),
                        Collectors.summingDouble(score -> score.getWeightedScore() != null ? score.getWeightedScore() : 0)));

        return totalBySubjectAndTerm.values().stream()
                .mapToDouble(a -> a.doubleValue())
                .average()
                .orElse(0.0);
    }
}
