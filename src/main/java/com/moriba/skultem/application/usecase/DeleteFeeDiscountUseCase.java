package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a discount with everything it created: every fee row in its group, the link from the
 * student's fee records (so the fees go back to their full amount) and the ledger credits it posted.
 * Later ledger balances are recomputed because each stored balance builds on the entries before it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteFeeDiscountUseCase {

    private final FeeDiscountRepository repo;
    private final StudentFeeRepository studentFeeRepo;
    private final StudentLedgerEntryRepository ledgerRepo;
    private final RecomputeStudentLedgerBalancesUseCase recomputeBalances;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_DISCOUNT_DELETED")
    public void execute(String schoolId, String groupId) {
        var discounts = repo.findAllByGroupIdAndSchoolId(groupId, schoolId);
        if (discounts.isEmpty()) {
            throw new NotFoundException("Discount not found");
        }

        var student = discounts.get(0).getEnrollment().getStudent();
        var discountIds = discounts.stream().map(d -> d.getId()).collect(java.util.stream.Collectors.toSet());

        for (var discount : discounts) {
            studentFeeRepo
                    .findBySchoolAndEnrollmentAndFee(schoolId, discount.getEnrollment().getId(),
                            discount.getFee().getId(), Pageable.unpaged())
                    .getContent().stream()
                    .filter(sf -> sf.getDiscount() != null && discountIds.contains(sf.getDiscount().getId()))
                    .forEach(sf -> {
                        sf.clearDiscount();
                        studentFeeRepo.save(sf);
                    });
        }

        var credits = ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(student.getId(), schoolId)
                .stream()
                .filter(e -> e.getTransactionType() == TransactionType.DISCOUNT
                        && discountIds.contains(e.getReferenceId()))
                .toList();
        ledgerRepo.deleteAll(credits);

        repo.deleteAll(discounts);
        recomputeBalances.recomputeForStudent(student.getId(), schoolId);

        logActivityUseCase.log(schoolId, ActivityType.FEES, "Fee discount deleted",
                student.getGivenNames() + " " + student.getFamilyName() + " - " + discounts.get(0).getName(),
                null, groupId);
    }
}
