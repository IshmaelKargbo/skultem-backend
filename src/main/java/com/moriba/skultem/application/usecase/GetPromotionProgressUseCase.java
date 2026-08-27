package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionProgressDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * How far along the school is in promoting every class for the active academic year - drives the
 * admin's "Close Year & Activate Next" gate.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetPromotionProgressUseCase {

    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository classSessionRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final PromotionRequestRepository promotionRequestRepo;
    private final FeeStructureRepository feeStructureRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final PaymentRepository paymentRepo;

    public PromotionProgressDTO execute(String schoolId) {
        var activeYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException("Active academic year not found"));

        Set<String> approvedSessionIds = promotionRequestRepo
                .findByAcademicYearIdAndSchoolId(activeYear.getId(), schoolId).stream()
                .filter(PromotionRequest::isApproved)
                .map(r -> r.getSession().getId())
                .collect(Collectors.toSet());

        var sessions = classSessionRepo.findBySchoolIdAndAcademicYearId(schoolId, activeYear.getId(),
                Pageable.unpaged()).getContent();

        int completed = 0;
        int pending = 0;

        for (var session : sessions) {
            if (approvedSessionIds.contains(session.getId())) {
                completed++;
                continue;
            }

            var section = session.getSection();
            var stream = session.getStream();
            var active = enrollmentRepo.findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(
                    session.getClazz().getId(), section.getId(), stream != null ? stream.getId() : null,
                    activeYear.getId(), schoolId);

            if (!active.isEmpty()) {
                pending++;
            }
        }

        var nextYear = academicYearRepo.findNextBySchool(schoolId, activeYear.getEndDate());
        var outstandingPlatformFee = outstandingPlatformFee(schoolId, activeYear.getId());

        return new PromotionProgressDTO(
                activeYear.getName(),
                completed + pending,
                completed,
                pending,
                pending == 0 && completed > 0 && outstandingPlatformFee.compareTo(BigDecimal.ZERO) <= 0,
                nextYear.isPresent(),
                nextYear.map(y -> y.getName()).orElse(null),
                outstandingPlatformFee);
    }

    // The school itself owes this, not any one student - so it's summed across every student the
    // platform fee was assigned to for the year, not per-enrollment like a normal fee's balance.
    // No platform fee has been seeded yet (see SeedPlatformFeeForAcademicYearUseCase) is treated the
    // same as "nothing owed", not as a blocker - a school a SYSTEM_ADMIN hasn't priced yet shouldn't
    // be stuck unable to close its year.
    private BigDecimal outstandingPlatformFee(String schoolId, String academicYearId) {
        var platformFee = feeStructureRepo.findSystemFeeBySchoolAndAcademicYear(schoolId, academicYearId).orElse(null);
        if (platformFee == null) {
            return BigDecimal.ZERO;
        }

        long assignedCount = studentFeeRepo.countByFeeAndSchool(platformFee.getId(), schoolId);
        BigDecimal totalAssigned = platformFee.getAmount().multiply(BigDecimal.valueOf(assignedCount));

        BigDecimal totalPaid = paymentRepo.sumPaymentsByFeeAndSchool(platformFee.getId(), schoolId);
        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }

        BigDecimal outstanding = totalAssigned.subtract(totalPaid);
        return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
    }
}
