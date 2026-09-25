package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Undoes a withdrawal or expulsion: the student is active again and, if they had a place in the
// current academic year's class, gets it back. A place from an earlier year stays as history.
@Service
@Transactional
@RequiredArgsConstructor
public class ReinstateStudentUseCase {

    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final GetStudentUseCase getStudentUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_REINSTATED")
    public StudentDTO execute(String schoolId, String studentId) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        if (!student.hasLeft()) {
            throw new RuleException(student.getName() + " hasn't withdrawn or been expelled.");
        }

        student.reinstate();
        studentRepo.save(student);

        var activeYear = academicYearRepo.findActiveBySchool(schoolId);
        if (activeYear.isPresent()) {
            var left = enrollmentRepo.findAllByStudentAndSchoolIdAndStatus(studentId, schoolId,
                    Enrollment.Status.LEFT);
            for (var enrollment : left) {
                if (enrollment.getAcademicYear() != null
                        && activeYear.get().getId().equals(enrollment.getAcademicYear().getId())) {
                    enrollment.reactivate();
                    enrollmentRepo.save(enrollment);
                }
            }
        }

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student reinstated",
                student.getName() + " - " + student.getAdmissionNumber(), null, student.getId());

        return getStudentUseCase.execute(studentId, schoolId, null);
    }
}
