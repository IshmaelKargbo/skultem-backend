package com.moriba.skultem.application.usecase;

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
import com.moriba.skultem.domain.repository.AcademicYearRepository;
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
        private final StudentRepository studentRepo;
        private final AcademicYearRepository academicYearRepo;

        public BigDecimal totalCollected(String schoolId) {
                return Optional.ofNullable(paymentRepo.sumPaymentsBySchool(schoolId))
                                .orElse(BigDecimal.ZERO);
        }

        public List<OutstandingBalanceDTO> outstandingForStudent(String schoolId, String studentId) {

                var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new RuleException("Student not found"));

                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                var fees = feeRepo
                                .findBySchoolAndAcademic(schoolId, academicYear.getId(), Pageable.unpaged())
                                .getContent();

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
                                        fee.getTerm().getName());

                }).toList();
        }

        public List<OutstandingBalanceDTO> outstandingOnlyForStudent(String schoolId, String studentId) {

                var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new RuleException("Student not found"));

                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                var fees = feeRepo
                                .findBySchoolAndAcademic(schoolId, academicYear.getId(), Pageable.unpaged())
                                .getContent();

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
                                                        fee.getDueDate(), status, fee.getTerm().getName());
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
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

        public Page<PaymentDTO> paymentHistory(String schoolId, String studentId, int page, int size) {
                studentRepo.findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new RuleException("Student not found"));

                Pageable pageable = size > 0
                                ? PageRequest.of(page, size)
                                : Pageable.unpaged();

                return paymentRepo
                                .findByStudent(studentId, pageable)
                                .map(PaymentMapper::toDTO);
        }
}
