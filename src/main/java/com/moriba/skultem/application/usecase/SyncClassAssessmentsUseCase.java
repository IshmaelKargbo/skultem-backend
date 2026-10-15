package com.moriba.skultem.application.usecase;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Creates the assessments a class's students are missing. Safe to run any time: a student who already
 * has assessments for a term and subject is left untouched, so only the gaps are filled. Reports what
 * could not be created and why, instead of failing silently.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SyncClassAssessmentsUseCase {

    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;

    public record Result(int students, int studentsUpdated, int assessmentsCreated,
            Set<String> subjectsWithoutTeacher, int failed, String firstError) {
    }

    @AuditLogAnnotation(action = "CLASS_ASSESSMENTS_SYNCED")
    public Result execute(String schoolId, String classId) {
        var clazz = classRepository.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        if (clazz.getTemplate() == null) {
            throw new RuleException("This class has no assessment template. Pick a template first.");
        }

        var year = academicYearRepository.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        var enrollments = enrollmentRepository
                .findAllByClassAndAcademicAndSchoolId(classId, year.getId(), schoolId, Pageable.unpaged())
                .getContent().stream()
                .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE)
                .toList();

        int updated = 0, created = 0, failed = 0;
        String firstError = null;
        Set<String> noTeacher = new LinkedHashSet<>();

        for (var enrollment : enrollments) {
            try {
                var r = provisionStudentAssessmentsUseCase.execute(enrollment);
                noTeacher.addAll(r.subjectsWithoutTeacher());
                if (r.created() > 0) {
                    updated++;
                    created += r.created();
                }
            } catch (RuleException | NotFoundException e) {
                failed++;
                if (firstError == null) {
                    firstError = e.getMessage();
                }
            }
        }

        return new Result(enrollments.size(), updated, created, noTeacher, failed, firstError);
    }
}
