package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.repository.TransactionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Wipes a test school's own roster and activity data - every student, guardian, enrollment,
 * payment/fee charge, ledger entry, student transaction, attendance record, assessment score,
 * behaviour note and material issued to a student - so the school can go into production with a
 * clean slate instead of real students sitting alongside whatever was created while trying the
 * system out.
 *
 * Deliberately narrower than "everything": the school's own SETUP is left untouched (classes,
 * sections, streams, subjects, fee categories/structures, academic years/terms, grading scale,
 * branding, teachers/staff) - that's configuration the school did on purpose and would otherwise
 * have to redo from scratch before going live. Teacher/staff accounts and login users are also
 * left alone (a real staff member could have already been testing the system as themselves).
 *
 * Gated on {@link com.moriba.skultem.domain.model.School#isTestSchool()} - a school not flagged as
 * a test school can never be wiped through this use case, so this can't be pointed at live data by
 * a wrong id. One transaction, all-or-nothing: if any step fails, nothing is removed - a partial
 * wipe (orphaned rows left behind) would be worse than not wiping at all.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class WipeTestSchoolDataUseCase {

    private final SchoolRepository schoolRepo;

    // Roster/activity repositories, deleted in this order: activity records that reference a
    // student first, then the enrollment/guardian links, then the student and parent rows
    // themselves.
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final BehaviourRepository behaviourRepo;
    private final SupplyRepository supplyRepo;
    private final ReportCardRepository reportCardRepo;
    private final StudentLedgerEntryRepository studentLedgerEntryRepo;
    private final TransactionRepository transactionRepo;
    private final PaymentRepository paymentRepo;
    private final FeeDiscountRepository feeDiscountRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final AttendanceRepository attendanceRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final StudentParentRepository studentParentRepo;
    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;

    // @AuditLogAnnotation is Spring-AOP-proxy-based, so it only fires through an external call to
    // this public method - it can't be moved onto a private helper called from within this same
    // class (self-invocation bypasses the proxy entirely and the aspect would silently never run).
    @AuditLogAnnotation(action = "TEST_SCHOOL_DATA_WIPED")
    public SchoolDTO execute(String schoolId) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));

        if (!school.isTestSchool()) {
            throw new RuleException(
                    "Only a school flagged as a test school can have its data wiped - flag it first");
        }

        assessmentScoreRepo.deleteAllBySchoolId(schoolId);
        studentAssessmentRepo.deleteAllBySchoolId(schoolId);
        behaviourRepo.deleteAllBySchoolId(schoolId);
        supplyRepo.deleteAllBySchoolId(schoolId);
        reportCardRepo.deleteAllBySchoolId(schoolId);
        studentLedgerEntryRepo.deleteAllBySchoolId(schoolId);
        transactionRepo.deleteAllBySchoolIdAndReferenceType(schoolId, ReferenceType.STUDENT);
        paymentRepo.deleteAllBySchoolId(schoolId);
        feeDiscountRepo.deleteAllBySchoolId(schoolId);
        studentFeeRepo.deleteAllBySchoolId(schoolId);
        attendanceRepo.deleteAllBySchoolId(schoolId);
        enrollmentSubjectRepo.deleteAllBySchoolId(schoolId);
        enrollmentRepo.deleteAllBySchoolId(schoolId);
        studentParentRepo.deleteAllBySchoolId(schoolId);
        studentRepo.deleteAllBySchoolId(schoolId);
        parentRepo.deleteAllBySchoolId(schoolId);

        school.markAsTest(false);
        schoolRepo.save(school);

        return SchoolMapper.toDTO(school);
    }
}
