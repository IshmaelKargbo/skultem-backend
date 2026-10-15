package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateClassTemplateUseCase {

    private final ClassRepository classRepository;
    private final AssessmentTemplateRepository assessmentTemplateRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;

    @AuditLogAnnotation(action = "CLASS_TEMPLATE_UPDATED")
    public ClassDTO execute(String schoolId, String classId, String templateId) {
        var clazz = classRepository.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (assessmentScoreRepository.existsGradeActivityByClassIdAndSchoolId(classId, schoolId)) {
            throw new RuleException("Class template cannot be changed because grading has already started for this class");
        }

        var template = assessmentTemplateRepository.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));

        clazz.setTemplate(template);
        classRepository.save(clazz);

        // Students already in the class may have no assessments yet (the class had no template when
        // they were enrolled). Provisioning skips any student/term/subject that already has them.
        academicYearRepository.findActiveBySchool(schoolId).ifPresent(year -> {
            var enrollments = enrollmentRepository
                    .findAllByClassAndAcademicAndSchoolId(classId, year.getId(), schoolId, Pageable.unpaged())
                    .getContent();
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getStatus() != Enrollment.Status.ACTIVE) {
                    continue;
                }
                try {
                    provisionStudentAssessmentsUseCase.execute(enrollment);
                } catch (Exception e) {
                    // One student's missing setup (e.g. no teacher for a subject yet) must not block the rest.
                }
            }
        });

        return ClassMapper.toDTO(clazz);
    }
}
