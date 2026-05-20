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
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
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
    private final AcademicYearRepository academicYearRepo;
    private final StudentRepository studentRepo;
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final CreateTransactionUsercase createTransactionUsercase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PAYMENT_RECORDED")
    public List<PaymentDTO> execute(PaymentRecord param) {

        var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                .orElseThrow(() -> new NotFoundException("no academic year found"));

        var student = studentRepo.findByIdAndSchoolId(param.studentId(), param.schoolId())
                .orElseThrow(() -> new RuleException("student not found"));

        List<PaymentDTO> results = new ArrayList<>();

        for (var item : param.items()) {

            var fee = feeRepo.findByIdAndSchoolId(item.feeId(), param.schoolId())
                    .orElseThrow(() -> new RuleException("fee not found"));

            var paidSoFar = paymentRepo.sumPaymentsByStudentAndFee(
                    param.studentId(),
                    item.feeId());

            BigDecimal outstanding = fee.getAmount().subtract(paidSoFar);

            if (item.amount().compareTo(outstanding) > 0) {
                throw new RuleException(
                        "payment for " + fee.getCategory().getName()
                                + " cannot exceed outstanding balance");
            }

            var payment = Payment.create(
                    param.schoolId(),
                    student,
                    fee,
                    item.amount(),
                    param.method(),
                    param.referenceNo(),
                    param.note(),
                    Instant.now());

            paymentRepo.save(payment);

            String description = Generate.generateLedgerDescription(
                    TransactionType.PAYMENT,
                    fee.getTerm().getName(),
                    fee.getCategory().getName(),
                    student.getGivenNames(),
                    student.getFamilyName(),
                    student.getAdmissionNumber(),
                    item.amount());

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

            createTransactionUsercase.createEntry(
                    param.schoolId(),
                    com.moriba.skultem.domain.model.Transaction.TransactionType.PAYMENT,
                    com.moriba.skultem.domain.model.Transaction.Direction.CREDIT,
                    item.amount(),
                    payment.getId(),
                    ReferenceType.STUDENT);

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

            results.add(PaymentMapper.toDTO(payment));
        }

        return results;
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