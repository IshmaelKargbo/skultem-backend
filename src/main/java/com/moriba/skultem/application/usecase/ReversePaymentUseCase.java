package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.PaymentReversalRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.MoneyUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Reverses a payment that was recorded by mistake (the student never paid, or the wrong amount or fee was entered).
 * <ul>
 * <li>The payment is removed, so the fee is owed again and every balance, report and receipt total is right.</li>
 * <li>Its credit comes off the student's ledger and the running balances are recomputed.</li>
 * <li>The school's cashbook keeps the original entry and gets a matching ADJUSTMENT out, so the money history still
 * adds up.</li>
 * <li>What it was, who reversed it and why is kept in payment_reversals, the activity log and the audit log.</li>
 * </ul>
 * The platform fee is Skultem's, so its payments are not reversed here. Materials already handed out because the fee
 * was fully paid are not taken back automatically - the result says so.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReversePaymentUseCase {

    private final PaymentRepository paymentRepo;
    private final PaymentReversalRepository reversalRepo;
    private final StudentLedgerEntryRepository ledgerRepo;
    private final RecomputeStudentLedgerBalancesUseCase recomputeBalances;
    private final CreateTransactionUsercase createTransactionUsercase;
    private final LogActivityUseCase logActivityUseCase;

    public record Result(String paymentId, String studentId, BigDecimal amount, String fee, boolean hadSupplies) {
    }

    @AuditLogAnnotation(action = "PAYMENT_REVERSED")
    public Result execute(String schoolId, String studentId, String paymentId, String reason, String userId) {
        String why = reason == null ? "" : reason.trim();
        if (why.length() < 3) {
            throw new RuleException("Say why this payment is being reversed");
        }

        Payment payment = paymentRepo.findByIdAndSchoolId(paymentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payment not found - it may already have been reversed"));
        if (!payment.getStudent().getId().equals(studentId)) {
            throw new NotFoundException("Payment not found for this student");
        }
        var fee = payment.getFee();
        if (fee.isSystem()) {
            throw new RuleException("Platform fee payments can't be reversed here - contact Skultem support");
        }
        var student = payment.getStudent();
        String feeName = fee.getCategory().getName() + " (" + fee.getTerm().getName() + ")";
        Instant now = Instant.now();

        reversalRepo.record(payment, why, userId, now);

        ledgerRepo.deleteAll(ledgerRepo
                .findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(student.getId(), schoolId).stream()
                .filter(e -> e.getTransactionType() == TransactionType.PAYMENT && paymentId.equals(e.getReferenceId()))
                .toList());
        paymentRepo.delete(payment);
        recomputeBalances.recomputeForStudent(student.getId(), schoolId);

        createTransactionUsercase.createEntry(schoolId, Transaction.TransactionType.ADJUSTMENT,
                Transaction.Direction.DEBIT, payment.getAmount(), paymentId, Transaction.ReferenceType.STUDENT);

        logActivityUseCase.log(schoolId, ActivityType.FEES, "Payment reversed",
                MoneyUtil.format(payment.getAmount()) + " for " + student.getGivenNames() + " "
                        + student.getFamilyName() + " - " + feeName,
                "receipt=" + payment.getReferenceNo() + ";reason=" + why, paymentId);

        return new Result(paymentId, student.getId(), payment.getAmount(), feeName, fee.isHasSupply());
    }
}
