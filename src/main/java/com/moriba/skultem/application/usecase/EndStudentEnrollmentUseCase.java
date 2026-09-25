package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A student leaves the school without finishing: WITHDRAWN (enrollment stopped) or EXPELLED. Their
// enrollment becomes LEFT, so they drop out of class rosters, attendance, grading and new fee
// assignments - but the record, the fees they already owe and their history all stay, and
// ReinstateStudentUseCase can undo it.
@Service
@Transactional
@RequiredArgsConstructor
public class EndStudentEnrollmentUseCase {

    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final GetStudentUseCase getStudentUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_ENROLLMENT_ENDED")
    public StudentDTO execute(String schoolId, String studentId, Student.Status type, String reason, LocalDate date,
            String note) {
        if (type != Student.Status.WITHDRAWN && type != Student.Status.EXPELLED) {
            throw new RuleException("A student can only be withdrawn or expelled.");
        }

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        if (student.hasLeft()) {
            throw new RuleException(student.getName() + " has already been "
                    + student.getStatus().name().toLowerCase() + ". Reinstate them first to change that.");
        }
        if (student.getStatus() != Student.Status.ACTIVE && student.getStatus() != Student.Status.SUSPENDED) {
            throw new RuleException("Only a current student can be withdrawn or expelled.");
        }

        String cleanReason = blankToNull(reason);
        if (type == Student.Status.EXPELLED && cleanReason == null) {
            throw new RuleException("Give a reason for the expulsion.");
        }

        LocalDate effective = date != null ? date : LocalDate.now();
        if (effective.isAfter(LocalDate.now())) {
            throw new RuleException("The date can't be in the future.");
        }

        student.leaveSchool(type, cleanReason, effective, blankToNull(note));
        studentRepo.save(student);

        var active = enrollmentRepo.findAllByStudentAndSchoolIdAndStatus(studentId, schoolId,
                Enrollment.Status.ACTIVE);
        for (var enrollment : active) {
            enrollment.leave();
            enrollmentRepo.save(enrollment);
        }

        String verb = type == Student.Status.EXPELLED ? "expelled" : "withdrawn";
        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student " + verb,
                student.getName() + " - " + student.getAdmissionNumber(),
                cleanReason == null ? null : "Reason: " + cleanReason, student.getId());

        return getStudentUseCase.execute(studentId, schoolId, null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
