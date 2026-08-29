package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PaymentMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.FeeDiscount.Kind;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;
import com.moriba.skultem.utils.MoneyUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecordPaymentUseCase {

        private final PaymentRepository paymentRepo;
        private final FeeStructureRepository feeRepo;
        private final FeeDiscountRepository discountRepo;
        private final AcademicYearRepository academicYearRepo;
        private final StudentRepository studentRepo;
        private final SupplyRepository supplyRepo;

        private final CreateSupplyUseCase createSupplyUseCase;
        private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
        private final CreateTransactionUsercase createTransactionUsercase;
        private final ReferenceGeneratorUsecase referenceGeneratorUsecase;
        private final LogActivityUseCase logActivityUseCase;

        @AuditLogAnnotation(action = "PAYMENT_RECORDED")
        public List<PaymentDTO> execute(PaymentRecord param) {

                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                                .orElseThrow(() -> new NotFoundException("No academic year found"));

                var student = studentRepo.findByIdAndSchoolId(
                                param.studentId(),
                                param.schoolId())
                                .orElseThrow(() -> new RuleException("Student not found"));

                String receiptNo = referenceGeneratorUsecase.generateTimeSeriesForSchool("PAYMENT_RECEIPT", "RCT",
                                param.schoolId());

                List<PaymentDTO> results = new ArrayList<>();

                for (var item : param.items()) {

                        var fee = feeRepo.findByIdAndSchoolId(
                                        item.feeId(),
                                        param.schoolId())
                                        .orElseThrow(() -> new RuleException("Fee not found"));

                        BigDecimal outstanding = calculateOutstanding(fee, param.studentId());

                        // FULLY PAID ALREADY
                        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                                throw new RuleException(
                                                fee.getCategory().getName()
                                                                + " already fully paid");
                        }

                        // INVALID AMOUNT
                        if (item.amount() == null
                                        || item.amount().compareTo(BigDecimal.ZERO) <= 0) {

                                throw new RuleException(
                                                "Payment amount must be greater than zero");
                        }

                        // OVERPAYMENT PROTECTION
                        if (item.amount().compareTo(outstanding) > 0) {
                                throw new RuleException(
                                                "Payment for "
                                                                + fee.getCategory().getName()
                                                                + " cannot exceed outstanding balance");
                        }

                        // INSTALLMENTS NOT ALLOWED - this fee has to be paid off in one go, so a partial
                        // amount (leaving anything still outstanding) is rejected rather than silently
                        // accepted as the first of several installments.
                        if (!fee.isAllowInstallment() && item.amount().compareTo(outstanding) < 0) {
                                throw new RuleException(
                                                fee.getCategory().getName()
                                                                + " does not allow installments - pay the full outstanding balance of "
                                                                + MoneyUtil.format(outstanding));
                        }

                        // CREATE PAYMENT
                        var payment = Payment.create(
                                        param.schoolId(),
                                        student,
                                        fee,
                                        item.amount(),
                                        param.method(),
                                        receiptNo,
                                        param.referenceNo(),
                                        param.note(),
                                        Instant.now());

                        paymentRepo.save(payment);

                        // LEDGER DESCRIPTION
                        String description = Generate.generateLedgerDescription(
                                        TransactionType.PAYMENT,
                                        fee.getTerm().getName(),
                                        fee.getCategory().getName(),
                                        student.getGivenNames(),
                                        student.getFamilyName(),
                                        student.getAdmissionNumber(),
                                        item.amount());

                        // STUDENT LEDGER
                        createStudentLedgerUsercase.createEntry(
                                        param.schoolId(),
                                        academicYear.getId(),
                                        student.getId(),
                                        fee.getTerm().getId(),
                                        TransactionType.PAYMENT,
                                        Direction.CREDIT,
                                        item.amount(),
                                        payment.getId(),
                                        description,
                                        Instant.now());

                        // TRANSACTION ENTRY
                        createTransactionUsercase.createEntry(
                                        param.schoolId(),
                                        com.moriba.skultem.domain.model.Transaction.TransactionType.PAYMENT,
                                        com.moriba.skultem.domain.model.Transaction.Direction.CREDIT,
                                        item.amount(),
                                        payment.getId(),
                                        ReferenceType.STUDENT);

                        // ACTIVITY LOG
                        var currency = MoneyUtil.format(item.amount());

                        logActivityUseCase.log(
                                        param.schoolId(),
                                        ActivityType.FEES,
                                        fee.getTerm().getName() + " fees collected",
                                        currency + " from "
                                                        + student.getGivenNames() + " "
                                                        + student.getFamilyName(),
                                        null,
                                        payment.getId());

                        processSupply(fee, student);

                        results.add(PaymentMapper.toDTO(payment));
                }

                return results;
        }

        public void processSupply(FeeStructure fee, Student student) {

                if (!fee.isHasSupply()) {
                        return;
                }

                BigDecimal outstanding = calculateOutstanding(fee, student.getId());

                // FULLY PAID
                if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
                        boolean alreadyIssued = supplyRepo.existsByStudentIdAndMaterialIdAndSchoolId(
                                        student.getId(),
                                        fee.getMaterial().getId(),
                                        fee.getSchoolId());

                        if (alreadyIssued) {
                                return;
                        }

                        createSupplyUseCase.execute(
                                        fee.getSchoolId(),
                                        student.getId(),
                                        fee.getMaterial().getId(),
                                        fee.getTotalSupply());
                }
        }

        // What's actually still owed on this fee: the amount, less any discounts applied to it, less
        // whatever's already been paid - matching FinanceReportUseCase's outstanding-balance formula.
        // Ignoring discounts here (as this used to) let a payment be accepted past what the student
        // genuinely still owed, and separately meant a discounted student who paid their full
        // (reduced) balance was never detected as "fully paid" - so their supply never got issued.
        private BigDecimal calculateOutstanding(FeeStructure fee, String studentId) {
                BigDecimal discount = discountRepo.findBySchoolAndStudentIdAndFeeId(fee.getSchoolId(), studentId, fee.getId())
                                .stream()
                                .map(d -> d.getKind() == Kind.PERCENTAGE
                                                ? fee.getAmount().multiply(d.getValue())
                                                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                                                : d.getValue())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal paid = java.util.Optional
                                .ofNullable(paymentRepo.sumPaymentsByStudentAndFee(studentId, fee.getId()))
                                .orElse(BigDecimal.ZERO);

                BigDecimal outstanding = fee.getAmount().subtract(discount).subtract(paid);
                return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
        }

        public record PaymentRecord(
                        String schoolId,
                        String studentId,
                        List<FeeRecord> items,
                        PaymentMethod method,
                        String referenceNo,
                        String note) {
        }

        public record FeeRecord(
                        String feeId,
                        BigDecimal amount) {
        }
}
