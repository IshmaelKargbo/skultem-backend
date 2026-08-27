package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Seeds the platform fee for a school's academic year, once - see {@link FeeStructure#system}.
 * Triggered from {@link ActivateTermUseCase} on whichever term of the academic year activates
 * first, since that's the earliest point a term (required by {@code FeeStructure}) is guaranteed
 * to exist; an {@code existsSystemFeeBySchoolAndAcademicYear} check keeps it a no-op on every
 * later term activation for the same academic year.
 * <p>
 * Like any other fee structure, this only assigns the fee to students enrolled at the moment it
 * runs - a student who enrolls afterwards doesn't retroactively get it, matching how
 * {@link CreateFeeStructureUseCase} already behaves for school-created fees.
 * <p>
 * A school gets no platform fee at all until a SYSTEM_ADMIN has set an amount via
 * {@link UpdatePlatformFeeSettingUseCase} - this is a deliberate no-op, not a failure, so schools
 * aren't blocked from activating a term before that amount exists.
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
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final LogActivityUseCase logActivityUseCase;

    public void execute(String schoolId, AcademicYear academicYear, Term term) {
        var setting = settingRepo.find().orElse(null);
        if (setting == null || !setting.isConfigured()) {
            return;
        }

        if (feeStructureRepo.existsSystemFeeBySchoolAndAcademicYear(schoolId, academicYear.getId())) {
            return;
        }

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

        var enrollments = enrollmentRepo.findAllByAcademicSchoolId(academicYear.getId(), schoolId);
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
                    category.getName(),
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

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Platform fee seeded",
                academicYear.getName(),
                "assignedCount=" + assignedCount + ";amount=" + fee.getAmount(),
                fee.getId());
    }
}
