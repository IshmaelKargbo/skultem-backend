package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Sort;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.OutstandingBalanceDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PaymentMapper;
import com.moriba.skultem.domain.model.FeeDiscount.Kind;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FinanceReportUseCase {

        private final PaymentRepository paymentRepo;
        private final FeeStructureRepository feeRepo;
        private final FeeDiscountRepository discountRepo;
        private final EnrollmentRepository enrollmentRepo;
        private final StudentRepository studentRepo;
        private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

        // School fee revenue only - the platform/Skultem fee is charged and collected through the
        // same Payment table (see FeeStructure#system) but isn't the school's own money, so it's
        // excluded here exactly as it already is from the student ledger (StudentLedgerReportUseCase).
        public BigDecimal totalCollected(String schoolId) {
                return Optional.ofNullable(paymentRepo.sumSchoolPaymentsBySchool(schoolId))
                                .orElse(BigDecimal.ZERO);
        }

        public List<OutstandingBalanceDTO> outstandingForStudent(String schoolId, String studentId,
                        String academicYearId) {

                var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new RuleException("Student not found"));

                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                var enrollment = enrollmentRepo
                                .findByStudentAndAcademicYearAndSchoolId(studentId, academicYear.getId(), schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                var fees = feeRepo
                                .findApplicableFees(schoolId, academicYear.getId(), enrollment.getClazz().getId());

                LocalDate today = LocalDate.now();

                return fees.stream().map(fee -> {

                        BigDecimal paid = Optional.ofNullable(
                                        paymentRepo.sumPaymentsByStudentAndFee(student.getId(), fee.getId()))
                                        .orElse(BigDecimal.ZERO);

                        BigDecimal totalDiscount = calculateTotalDiscount(schoolId, student.getId(),
                                        fee.getId(), fee.getAmount());

                        BigDecimal outstanding = calculateOutstanding(fee.getAmount(), totalDiscount,
                                        paid);

                        String status = parseStatus(fee.getAmount(), totalDiscount, paid, fee.getDueDate(), today);

                        return new OutstandingBalanceDTO(
                                        fee.getId(),
                                        fee.getCategory().getName(),
                                        fee.getAmount(),
                                        paid,
                                        outstanding,
                                        totalDiscount,
                                        fee.getDueDate(),
                                        status,
                                        fee.getTerm().getName(),
                                        fee.isAllowInstallment());

                }).toList();
        }

        public List<OutstandingBalanceDTO> outstandingOnlyForStudent(String schoolId, String studentId,
                        String academicYearId) {

                var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new RuleException("Student not found"));

                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                var enrollment = enrollmentRepo
                                .findByStudentAndAcademicYearAndSchoolId(studentId, academicYear.getId(), schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                var fees = feeRepo
                                .findApplicableFees(schoolId, academicYear.getId(), enrollment.getClazz().getId());

                LocalDate today = LocalDate.now();

                return fees.stream()
                                .map(fee -> {

                                        BigDecimal paid = Optional.ofNullable(
                                                        paymentRepo.sumPaymentsByStudentAndFee(student.getId(),
                                                                        fee.getId()))
                                                        .orElse(BigDecimal.ZERO);

                                        BigDecimal totalDiscount = calculateTotalDiscount(schoolId,
                                                        student.getId(), fee.getId(), fee.getAmount());

                                        BigDecimal outstanding = calculateOutstanding(
                                                        fee.getAmount(),
                                                        totalDiscount,
                                                        paid);

                                        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                                                return null;
                                        }

                                        String status = determineStatus(outstanding, paid, fee.getDueDate(), today);

                                        return new OutstandingBalanceDTO(fee.getId(), fee.getCategory().getName(),
                                                        fee.getAmount(), paid, outstanding, totalDiscount,
                                                        fee.getDueDate(), status, fee.getTerm().getName(),
                                                        fee.isAllowInstallment());
                                })
                                .filter(Objects::nonNull)
                                .toList();
        }

        private BigDecimal calculateTotalDiscount(String schoolId, String studentId, String feeId,
                        BigDecimal feeAmount) {
                var discounts = discountRepo
                                .findBySchoolAndStudentIdAndFeeId(schoolId, studentId, feeId);

                return discounts.stream()
                                .map(d -> {
                                        if (d.getKind() == Kind.PERCENTAGE) {
                                                return feeAmount
                                                                .multiply(d.getValue())
                                                                .divide(BigDecimal.valueOf(100), 2,
                                                                                RoundingMode.HALF_UP);
                                        }
                                        return d.getValue();
                                })
                                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        }

        private BigDecimal calculateOutstanding(BigDecimal feeAmount, BigDecimal totalDiscount, BigDecimal paid) {

                BigDecimal result = feeAmount
                                .subtract(totalDiscount)
                                .subtract(paid);

                return result.compareTo(BigDecimal.ZERO) < 0
                                ? BigDecimal.ZERO
                                : result;
        }

        private String determineStatus(BigDecimal outstanding,
                        BigDecimal paid, LocalDate dueDate, LocalDate today) {

                if (paid.compareTo(BigDecimal.ZERO) == 0)
                        return "Unpaid";

                if (outstanding.compareTo(BigDecimal.ZERO) > 0 && dueDate.isBefore(today))
                        return "Overdue";

                return "Partial";
        }

        private String parseStatus(
                        BigDecimal feeAmount,
                        BigDecimal totalDiscount,
                        BigDecimal paid,
                        LocalDate dueDate,
                        LocalDate today) {

                BigDecimal netPayable = feeAmount.subtract(totalDiscount);

                if (netPayable.compareTo(BigDecimal.ZERO) <= 0) {
                        return "Paid";
                }

                BigDecimal outstanding = netPayable.subtract(paid);

                if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                        return "Paid";
                }

                if (paid.compareTo(BigDecimal.ZERO) == 0) {
                        return "Unpaid";
                }

                if (dueDate.isBefore(today)) {
                        return "Overdue";
                }

                return "Partial";
        }

        /**
         * A student's payments for one academic year - the one asked for, or the school's active year when
         * none is given - newest first, not every payment they've ever made. A school with no active year has
         * no payments to show (an empty page, not an error); a year that was asked for but doesn't exist is.
         */
        public Page<PaymentDTO> paymentHistory(String schoolId, String studentId, String academicYearId, int page,
                        int size) {
                studentRepo.findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new RuleException("Student not found"));

                Pageable pageable = size > 0
                                ? PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                                : Pageable.unpaged();

                AcademicYear academicYear;
                try {
                        academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
                } catch (NotFoundException e) {
                        if (academicYearId != null && !academicYearId.isBlank()) {
                                throw e;
                        }
                        return Page.empty(pageable);
                }

                return paymentRepo
                                .findByStudentAndAcademicYear(studentId, academicYear.getId(), pageable)
                                .map(PaymentMapper::toDTO);
        }
}
