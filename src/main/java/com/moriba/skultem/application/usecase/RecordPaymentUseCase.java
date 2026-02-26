package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PaymentMapper;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.utils.Generate;

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
        private final ReferenceGeneratorUsecase rg;

        public PaymentDTO execute(PaymentRecord param) {
                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                                .orElseThrow(() -> new NotFoundException("no academic year found"));
                var student = studentRepo.findByIdAndSchoolId(param.studentId(), param.schoolId())
                                .orElseThrow(() -> new RuleException("student not found"));
                var fee = feeRepo.findByIdAndSchoolId(param.feeId(), param.schoolId())
                                .orElseThrow(() -> new RuleException("fee not found"));

                var paidSoFar = paymentRepo.sumPaymentsByStudentAndFee(param.studentId(), param.feeId());
                var outstanding = fee.getAmount().subtract(paidSoFar);

                if (param.amount.compareTo(outstanding) > 0) {
                        throw new RuleException("payment cannot exceed outstanding balance");
                }

                var id = rg.generate("PAYMENT", "PAY");
                var payment = Payment.create(id, param.schoolId(), student, fee, param.amount(), param.method(),
                                param.referenceNo(), param.note(), Instant.now());
                paymentRepo.save(payment);

                var description = Generate.generateLedgerDescription(TransactionType.PAYMENT,
                                fee.getTerm().getName(), fee.getCategory().getName(), student.getGivenNames(),
                                student.getFamilyName(), student.getAdmissionNumber(), param.amount());

               
                createStudentLedgerUsercase.createEntry(param.schoolId(), academicYear.getId(),
                                student.getId(), fee.getTerm().getId(), TransactionType.PAYMENT,
                                Direction.CREDIT, param.amount, id, description, Instant.now());

                return PaymentMapper.toDTO(payment);
        }

        public record PaymentRecord(String schoolId, String studentId, String feeId, BigDecimal amount,
                        PaymentMethod method, String referenceNo, String note) {
        }
}
