package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.model.FeeDiscount.Kind;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateFeeDiscountUseCase {

        private final EnrollmentRepository enrollmentRepo;
        private final AcademicYearRepository academicYearRepo;
        private final StudentRepository studentRepo;
        private final FeeStructureRepository feeStructureRepo;
        private final FeeDiscountRepository repo;
        private final PaymentRepository paymentRepo;
        private final StudentFeeRepository studentFeeRepo;
        private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
        private final LogActivityUseCase logActivityUseCase;

        @AuditLogAnnotation(action = "FEE_DISCOUNT_CREATED")
        public void execute(DiscountRecord param) {

                var student = studentRepo.findByIdAndSchoolId(param.studentId(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Student not found"));

                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                var enrollment = enrollmentRepo
                                .findByStudentAndAcademicYearAndSchoolId(
                                                param.studentId(),
                                                academicYear.getId(),
                                                param.schoolId())
                                .orElseThrow(() -> new NotFoundException(
                                                "Student not enrolled in active academic year"));

                var items = param.fees().stream().collect(java.util.stream.Collectors.toMap(
                                FeeValue::feeId, f -> f, (a, b) -> a, java.util.LinkedHashMap::new)).values();
                if (items.isEmpty()) {
                        throw new IllegalArgumentException("Select at least one fee");
                }

                // One discount, one group: every fee row below shares it, so the discount is listed and
                // deleted as a unit. All-or-nothing - any failing fee rolls back the rest.
                var groupId = UUID.randomUUID().toString();
                var studentName = student.getGivenNames() + " " + student.getFamilyName();

                for (FeeValue item : items) {
                        String feeId = item.feeId();
                        BigDecimal value = item.value();
                        var fee = feeStructureRepo.findByIdAndSchoolId(feeId, param.schoolId())
                                        .orElseThrow(() -> new NotFoundException("Fee structure not found"));
                        var feeLabel = fee.getTerm().getName() + " " + fee.getCategory().getName();

                        // The discount lives on the student's own fee record, which holds a single one.
                        var studentFee = studentFeeRepo
                                        .findBySchoolAndEnrollmentAndFee(param.schoolId(), enrollment.getId(), feeId,
                                                        Pageable.unpaged())
                                        .getContent().stream().findFirst()
                                        .orElseThrow(() -> new NotFoundException(
                                                        studentName + " has not been charged " + feeLabel));

                        if (studentFee.getDiscount() != null
                                        || !repo.findBySchoolAndStudentIdAndFeeId(param.schoolId(), param.studentId(),
                                                        feeId).isEmpty()) {
                                throw new AlreadyExistsException(
                                                feeLabel + " already has a discount - delete it before adding another");
                        }

                        BigDecimal discountAmount = calculateDiscountAmount(param.kind(), value,
                                        fee.getAmount());

                        // Validated against what's still outstanding on this fee, so a discount can't push
                        // the student's ledger balance negative.
                        BigDecimal paid = java.util.Optional.ofNullable(
                                        paymentRepo.sumPaymentsByStudentAndFee(param.studentId(), feeId))
                                        .orElse(BigDecimal.ZERO);

                        BigDecimal outstanding = fee.getAmount().subtract(paid);
                        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                                outstanding = BigDecimal.ZERO;
                        }

                        if (discountAmount.compareTo(outstanding) > 0) {
                                throw new IllegalArgumentException(
                                                "Discount cannot exceed the outstanding balance of " + outstanding
                                                                + " on " + feeLabel);
                        }

                        var id = UUID.randomUUID().toString();
                        var feeDiscount = FeeDiscount.create(id, param.schoolId(), groupId, param.name(),
                                        param.kind(), value, student, param.expiryDate(), enrollment, fee,
                                        param.reason());

                        repo.save(feeDiscount);

                        studentFee.applyDiscount(feeDiscount);
                        studentFeeRepo.save(studentFee);

                        var description = Generate.generateLedgerDescription(TransactionType.DISCOUNT,
                                        fee.getTerm().getName(), fee.getCategory().getName(), student.getGivenNames(),
                                        student.getFamilyName(), student.getAdmissionNumber(), discountAmount);

                        createStudentLedgerUsercase.createEntry(param.schoolId(), academicYear.getId(),
                                        student.getId(), fee.getTerm().getId(), TransactionType.DISCOUNT,
                                        Direction.CREDIT, discountAmount, id, description, Instant.now());
                }

                logActivityUseCase.log(
                                param.schoolId(),
                                ActivityType.FEES,
                                "Fee discount created",
                                studentName + " - " + param.name() + " (" + items.size() + " fee"
                                                + (items.size() == 1 ? "" : "s") + ")",
                                null,
                                groupId);
        }

        private BigDecimal calculateDiscountAmount(Kind kind, BigDecimal value, BigDecimal feeAmount) {
                if (kind == Kind.PERCENTAGE) {
                        return feeAmount
                                        .multiply(value)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                return value;
        }

        public record FeeValue(String feeId, BigDecimal value) {
        }

        public record DiscountRecord(String schoolId, Kind kind, String name,
                        LocalDate expiryDate, String studentId, List<FeeValue> fees, String reason) {
        }
}
