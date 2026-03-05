package com.moriba.skultem.application.usecase;

import java.util.Comparator;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCycleOverviewDTO;
import com.moriba.skultem.application.dto.ClassAssessmentCycleStatusDTO;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetAssessmentCycleOverviewUseCase {

    private final ClassRepository classRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final TermRepository termRepository;

    public AssessmentCycleOverviewDTO execute(String schoolId) {
        var activeTerm = termRepository.findFirstBySchoolIdAndStatus(schoolId, Term.Status.ACTIVE)
                .map(TermMapper::toDTO)
                .orElse(null);

        var classes = classRepository.findBySchool(schoolId, Pageable.unpaged()).getContent().stream()
                .sorted(Comparator.comparingInt(clazz -> clazz.getDisplayOrder()))
                .map(clazz -> {
                    var template = clazz.getTemplate();
                    if (template == null) {
                        return new ClassAssessmentCycleStatusDTO(
                                clazz.getId(),
                                clazz.getName(),
                                null,
                                null,
                                0,
                                0,
                                false,
                                false,
                                "No template assigned");
                    }

                    var assessments = assessmentRepository.findAllByTemplateIdAndSchoolId(template.getId(), schoolId);
                    int totalWeight = assessments.stream()
                            .mapToInt(item -> item.getWeight() == null ? 0 : item.getWeight())
                            .sum();

                    boolean ready = activeTerm != null
                            && !assessments.isEmpty()
                            && totalWeight == 100;
                    boolean templateLocked = assessmentScoreRepository
                            .existsGradeActivityByClassIdAndSchoolId(clazz.getId(), schoolId);

                    String note = activeTerm == null ? "No active term"
                            : assessments.isEmpty() ? "No assessments"
                                    : totalWeight != 100 ? "Weight must equal 100%"
                                            : templateLocked ? "Template locked after grading started" : "Ready";

                    return new ClassAssessmentCycleStatusDTO(
                            clazz.getId(),
                            clazz.getName(),
                            template.getId(),
                            template.getName(),
                            assessments.size(),
                            totalWeight,
                            templateLocked,
                            ready,
                            note);
                })
                .toList();

        int totalClasses = classes.size();
        int readyClasses = (int) classes.stream().filter(ClassAssessmentCycleStatusDTO::ready).count();
        int notReadyClasses = totalClasses - readyClasses;

        return new AssessmentCycleOverviewDTO(activeTerm, totalClasses, readyClasses, notReadyClasses, classes);
    }
}
