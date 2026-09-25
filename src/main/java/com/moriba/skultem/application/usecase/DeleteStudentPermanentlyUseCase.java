package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.StudentPurgeRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// For a student entered by mistake: removes the student and everything recorded against them -
// enrollments, fees and the ledger, assessments and scores, attendance, report cards. It cannot be
// undone, so the caller has to type the student's admission number, and it refuses when money has
// been collected (use withdraw/expel for a student who really attended).
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteStudentPermanentlyUseCase {

    public record Result(String studentName, int enrollmentsRemoved, int feesRemoved, int assessmentsRemoved) {
    }

    private final StudentRepository studentRepo;
    private final StudentPurgeRepository purgeRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_DELETED_PERMANENTLY")
    public Result execute(String schoolId, String studentId, String confirmation) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        if (confirmation == null
                || !confirmation.trim().equalsIgnoreCase(student.getAdmissionNumber().trim())) {
            throw new RuleException("Type the student's admission number (" + student.getAdmissionNumber()
                    + ") to confirm the deletion.");
        }

        if (purgeRepo.hasRecordedPayments(schoolId, studentId)) {
            throw new RuleException(student.getName() + " has payments recorded, so they can't be deleted "
                    + "permanently. Withdraw or expel them instead - that keeps the financial record.");
        }

        var purged = purgeRepo.purge(schoolId, studentId);

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student permanently deleted",
                student.getName() + " - " + student.getAdmissionNumber(),
                purged.fees() + " fee(s), " + purged.assessments() + " assessment(s) removed", null);

        return new Result(student.getName(), purged.enrollments(), purged.fees(), purged.assessments());
    }
}
