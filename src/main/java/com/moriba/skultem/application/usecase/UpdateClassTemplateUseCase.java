package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.TemplateMaintenanceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Sets (or changes) a class's assessment template and brings its students' assessments in line straight away:
// - a first template creates every student's tests,
// - a different template replaces this year's tests (only before grading has started - nothing is lost),
// - the same template again just fills any gaps.
// The result says what happened, so the screen can tell the admin about subjects still waiting for a teacher.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateClassTemplateUseCase {

    public record Result(ClassDTO clazz, boolean replaced, SyncClassAssessmentsUseCase.Result sync) {
    }

    private final ClassRepository classRepository;
    private final AssessmentTemplateRepository assessmentTemplateRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TemplateMaintenanceRepository maintenance;
    private final SyncClassAssessmentsUseCase syncClassAssessmentsUseCase;

    @AuditLogAnnotation(action = "CLASS_TEMPLATE_UPDATED")
    public Result execute(String schoolId, String classId, String templateId) {
        var clazz = classRepository.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        var template = assessmentTemplateRepository.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));
        var year = academicYearRepository.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        boolean changing = clazz.getTemplate() != null && !clazz.getTemplate().getId().equals(templateId);

        // Only this year counts: last year's marks belong to last year and keep their own tests.
        if (changing && assessmentScoreRepository.existsGradeActivityByClassIdAndAcademicYearIdAndSchoolId(classId,
                year.getId(), schoolId)) {
            throw new RuleException("This class's template can't be changed because grading has already started "
                    + "this year. Edit the template itself instead, or change it before the next year.");
        }

        if (changing) {
            // The old template's tests haven't been touched - clear them so the new template's take their place.
            maintenance.deleteClassAssessments(classId, year.getId());
        }

        clazz.setTemplate(template);
        classRepository.save(clazz);

        var sync = syncClassAssessmentsUseCase.execute(schoolId, classId);
        return new Result(ClassMapper.toDTO(clazz), changing, sync);
    }
}
