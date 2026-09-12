package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.PlatformFeeSetting;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Seeds the platform fee for a school's academic year - see {@link FeeStructure#system}.
 * Triggered from {@link ActivateTermUseCase} on whichever term of the academic year activates
 * first, since that's the earliest point a term (required by {@code FeeStructure}) is guaranteed
 * to exist, and also from a startup sweep (see the platform-fee backfill listener) so a school
 * whose platform fee amount was only just configured - or whose enrollments grew since the last
 * run - doesn't have to wait for its next term activation to catch up.
 * <p>
 * Finds-or-creates the fee structure itself (once per school per academic year), then always
 * walks every currently-enrolled student and assigns whoever is still missing a
 * {@code StudentFee} for it - so this is safe, and useful, to call repeatedly: a student who
 * enrolled after the fee structure already existed (normally covered by
 * {@link ApplyApplicableFeesToEnrollmentUseCase} at enrollment time, but not if that call failed
 * or was skipped) still gets caught up here instead of silently missing the charge forever.
 * <p>
 * A school gets no platform fee at all until it has a configured amount - see
 * {@link PlatformFeeSetting} and {@link EnsurePlatformFeeSettingUseCase} - which is a deliberate
 * no-op, not a failure, so schools aren't blocked from activating a term before that amount
 * exists. Each school's amount is independent.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SeedPlatformFeeForAcademicYearUseCase {

    // Public so UpdateFeeCategoryUseCase/DeleteFeeCategoryUseCase can recognize and protect this
    // category by the same name this class looks it up by - renaming it out from under that lookup
    // would make every later academic year silently create a second, drifted "Platform Fee" category.
    public static final String PLATFORM_FEE_CATEGORY_NAME = "Platform Fee";

    private final PlatformFeeSettingRepository settingRepo;
    private final FeeStructureRepository feeStructureRepo;
    private final FeeCategoryRepository feeCategoryRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final TermRepository termRepo;
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final LogActivityUseCase logActivityUseCase;

    // Resolves the school's own active term/academic year and runs the same logic below - all in
    // this one transaction, so the Term entity's lazy academicYear association is dereferenced
    // while its Hibernate session is still open. Used by the platform-fee reconcile path (see
    // BackfillPlatformFeesUseCase), which - unlike ActivateTermUseCase - doesn't already have a
    // Term/AcademicYear in hand from its own transactional context; passing a lazy proxy fetched
    // in a since-closed transaction across method calls throws "no session" the moment it's
    // touched here instead.
    public void execute(String schoolId) {
        var activeTerm = termRepo.findFirstBySchoolIdAndStatus(schoolId, Term.Status.ACTIVE).orElse(null);
        if (activeTerm == null) {
            return;
        }

        execute(schoolId, activeTerm.getAcademicYear(), activeTerm);
    }

    public void execute(String schoolId, AcademicYear academicYear, Term term) {
        var setting = settingRepo.findBySchool(schoolId).orElse(null);
        if (setting == null || !setting.isConfigured()) {
            return;
        }

        var existingFee = feeStructureRepo.findSystemFeeBySchoolAndAcademicYear(schoolId, academicYear.getId());
        boolean feeJustCreated = existingFee.isEmpty();
        var fee = existingFee.orElseGet(() -> createPlatformFeeStructure(schoolId, term, academicYear, setting));

        var enrollments = enrollmentRepo.findAllByAcademicSchoolId(academicYear.getId(), schoolId);

        // Fast path for the overwhelmingly common steady state (every enrolled student already has
        // this fee): one COUNT instead of one existence check per enrollment. This runs on every
        // throttled reconcile triggered by a plain "get school info" request (see
        // BackfillPlatformFeesUseCase) - without this, a school with hundreds of students would
        // pay hundreds of extra round trips, on a background thread, on every single call, for a
        // sweep that (almost always) finds nothing to do.
        if (!feeJustCreated && studentFeeRepo.countByFeeAndSchool(fee.getId(), schoolId) >= enrollments.size()) {
            return;
        }

        int assignedCount = 0;

        for (var enrollment : enrollments) {
            if (studentFeeRepo.existsBySchoolAndEnrollmentAndStudentAndFee(schoolId, enrollment.getId(),
                    enrollment.getStudent().getId(), fee.getId())) {
                continue;
            }

            var studentFee = StudentFee.create(schoolId, enrollment, enrollment.getStudent(), fee, null);
            studentFeeRepo.save(studentFee);

            var description = Generate.generateLedgerDescription(
                    TransactionType.FEE_ASSINMENT,
                    term.getName(),
                    fee.getCategory().getName(),
                    enrollment.getStudent().getGivenNames(),
                    enrollment.getStudent().getFamilyName(),
                    enrollment.getStudent().getAdmissionNumber(),
                    fee.getAmount());

            createStudentLedgerUsercase.createEntry(
                    schoolId,
                    academicYear.getId(),
                    enrollment.getStudent().getId(),
                    term.getId(),
                    TransactionType.FEE_ASSINMENT,
                    Direction.DEBIT,
                    fee.getAmount(),
                    fee.getId(),
                    description,
                    Instant.now());

            assignedCount++;
        }

        // Only worth a log entry when something actually changed - this is now called from a
        // startup sweep too, which would otherwise write a no-op "seeded" entry for every school
        // on every restart.
        if (feeJustCreated || assignedCount > 0) {
            logActivityUseCase.log(
                    schoolId,
                    ActivityType.FEES,
                    feeJustCreated ? "Platform fee seeded" : "Platform fee backfilled",
                    academicYear.getName(),
                    "assignedCount=" + assignedCount + ";amount=" + fee.getAmount(),
                    fee.getId());
        }
    }

    private FeeStructure createPlatformFeeStructure(String schoolId, Term term, AcademicYear academicYear,
            PlatformFeeSetting setting) {
        var category = feeCategoryRepo.findByNameAndSchool(PLATFORM_FEE_CATEGORY_NAME, schoolId)
                .orElseGet(() -> {
                    var created = FeeCategory.create(UUID.randomUUID().toString(), schoolId,
                            PLATFORM_FEE_CATEGORY_NAME, "Platform subscription fee");
                    feeCategoryRepo.save(created);
                    return created;
                });

        var fee = FeeStructure.createSystemFee(schoolId, term, category, academicYear, term.getEndDate(),
                setting.getAmount(), "Platform fee");
        feeStructureRepo.save(fee);
        return fee;
    }
}
