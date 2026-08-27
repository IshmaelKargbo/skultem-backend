package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ActiveAssessmentCycleDTO;
import com.moriba.skultem.application.dto.AssessmentCycleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AssessmentCycleMapper;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetActiveAssessmentCycleUseCase {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INACTIVE_STATUS = "INACTIVE";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String PENDING_TERM_STATUS = "PENDING_TERM_ACTIVATION";

    private final ResolveActiveTermUseCase resolveActiveTermUseCase;
    private final AssessmentTemplateRepository templateRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassSubjectAssessmentLifeCycleRepository assessmentLifeCycleRepo;
    private final ClassRepository classRepository;

    public ActiveAssessmentCycleDTO execute(String schoolId, String classId) {
        return execute(schoolId, classId, null);
    }

    /**
     * @param academicYearId lets an admin view another year's cycle (e.g. a closed one) without
     *                       changing what's active for the rest of the school.
     */
    public ActiveAssessmentCycleDTO execute(String schoolId, String classId, String academicYearId) {

        var activeTerm = resolveActiveTermUseCase.execute(schoolId, academicYearId)
                .map(TermMapper::toDTO)
                .orElse(null);

        if (classId != null && !classId.isBlank()) {

            var clazz = classRepository
                    .findByIdAndSchool(classId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class not found"));

            var template = clazz.getTemplate();

            if (template == null) {
                return new ActiveAssessmentCycleDTO(activeTerm, null, null, null, List.of(), 0, false);
            }

            if (activeTerm == null) {
                return new ActiveAssessmentCycleDTO(null, template.getId(), template.getName(),
                        template.getDescription(), List.of(), 0, false);
            }

            var assessments = assessmentRepository
                    .findAllByTemplateIdAndSchoolId(template.getId(), schoolId)
                    .stream()
                    .sorted(Comparator.comparingInt(a -> a.getPosition()))
                    .toList();

            var lifeCycles = assessmentLifeCycleRepo.findAllBySchoolAndTerm(schoolId, activeTerm.id());

            int activePosition = -1;
            // An assessment with no life cycle rows yet (never started) must not read as
            // "completed" via allMatch's vacuous truth on an empty stream - only assessments that
            // actually have cycles, all of them COMPLETED, count as done.
            boolean allCompleted = !lifeCycles.isEmpty();

            for (var assessment : assessments) {

                var matching = lifeCycles.stream()
                        .filter(lc -> lc.getAssessment().getId().equals(assessment.getId()))
                        .toList();
                boolean completed = !matching.isEmpty()
                        && matching.stream().allMatch(lc -> lc.getStatus().name().equals("COMPLETED"));

                if (!completed) {
                    activePosition = assessment.getPosition();
                    allCompleted = false;
                    break;
                }
            }

            List<AssessmentCycleDTO> cycle = new ArrayList<>();

            for (var assessment : assessments) {

                String status;

                if (allCompleted) {
                    status = COMPLETED_STATUS;
                } else if (assessment.getPosition() == activePosition) {
                    status = ACTIVE_STATUS;
                } else if (assessment.getPosition() < activePosition) {
                    status = COMPLETED_STATUS;
                } else {
                    status = INACTIVE_STATUS;
                }

                cycle.add(AssessmentCycleMapper.toDTO(assessment, status));
            }

            int totalWeight = assessments.stream()
                    .mapToInt(item -> {
                        Integer weight = item.getWeight();
                        return weight == null ? 0 : weight;
                    })
                    .sum();

            boolean ready = !cycle.isEmpty() && totalWeight == 100;

            return new ActiveAssessmentCycleDTO(
                    activeTerm,
                    template.getId(),
                    template.getName(),
                    template.getDescription(),
                    cycle,
                    totalWeight,
                    ready);
        }

        var templates = templateRepository
                .findAllBySchoolId(schoolId, Pageable.unpaged())
                .getContent();

        if (templates.isEmpty()) {
            return new ActiveAssessmentCycleDTO(activeTerm, null, null, null, List.of(), 0, false);
        }

        var selectedTemplate = templates.get(0);

        var selectedAssessments = assessmentRepository
                .findAllByTemplateIdAndSchoolId(selectedTemplate.getId(), schoolId);

        for (var template : templates) {

            var candidate = assessmentRepository
                    .findAllByTemplateIdAndSchoolId(template.getId(), schoolId);

            if (!candidate.isEmpty()) {
                selectedTemplate = template;
                selectedAssessments = candidate;
                break;
            }
        }

        final String cycleStatus = activeTerm != null
                ? ACTIVE_STATUS
                : PENDING_TERM_STATUS;

        List<AssessmentCycleDTO> assessments = selectedAssessments.stream()
                .sorted(Comparator.comparingInt(a -> a.getPosition()))
                .map(assessment -> AssessmentCycleMapper.toDTO(assessment, cycleStatus))
                .toList();

        int totalWeight = selectedAssessments.stream()
                .mapToInt(item -> {
                    Integer weight = item.getWeight();
                    return weight == null ? 0 : weight;
                })
                .sum();

        boolean ready = activeTerm != null && !assessments.isEmpty() && totalWeight == 100;

        return new ActiveAssessmentCycleDTO(
                activeTerm,
                selectedTemplate.getId(),
                selectedTemplate.getName(),
                selectedTemplate.getDescription(),
                assessments,
                totalWeight,
                ready);
    }
}