package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

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
        private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
        private final ReferenceGeneratorUsecase rg;
        private final LogActivityUseCase logActivityUseCase;

        @AuditLogAnnotation(action = "FEE_DISCOUNT_CREATED")
        public void execute(DiscountRecord param) {

                var student = studentRepo.findByIdAndSchoolId(param.studentId(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Student not found"));

                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                var fee = feeStructureRepo.findByIdAndSchoolId(param.feeId(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Fee structure not found"));

                var enrollment = enrollmentRepo
                                .findByStudentAndAcademicYearAndSchoolId(
                                                param.studentId(),
                                                academicYear.getId(),
                                                param.schoolId())
                                .orElseThrow(() -> new NotFoundException(
                                                "Student not enrolled in active academic year"));

                if (repo.existsByNameAndEnrollmentAndFeeAndSchoolId(
                                param.name(),
                                enrollment.getId(),
                                fee.getId(),
                                param.schoolId())) {
                        throw new AlreadyExistsException("Discount already exists for this fee and student");
                }

                BigDecimal discountAmount = calculateDiscountAmount(
                                param.kind(),
                                param.value(),
                                fee.getAmount());

                if (discountAmount.compareTo(fee.getAmount()) > 0) {
                        throw new IllegalArgumentException("Discount cannot exceed fee amount");
                }

                var id = rg.generate("FEE_DISCOUNT", "FED");

                var feeDiscount = FeeDiscount.create(id, param.schoolId(), param.name(), param.kind(),
                                param.value(), student, param.expiryDate(), enrollment, fee, param.reason);

                repo.save(feeDiscount);

                logActivityUseCase.log(
                                param.schoolId(),
                                ActivityType.FEES,
                                "Fee discount created",
                                student.getGivenNames() + " " + student.getFamilyName() + " - " + fee.getCategory().getName(),
                                null,
                                feeDiscount.getId());

                var description = Generate.generateLedgerDescription(TransactionType.DISCOUNT,
                                fee.getTerm().getName(), fee.getCategory().getName(), student.getGivenNames(),
                                student.getFamilyName(), student.getAdmissionNumber(), discountAmount);

                createStudentLedgerUsercase.createEntry(param.schoolId(), academicYear.getId(),
                                student.getId(), fee.getTerm().getId(), TransactionType.DISCOUNT,
                                Direction.CREDIT, discountAmount, id, description, Instant.now());
        }

        private BigDecimal calculateDiscountAmount(Kind kind, BigDecimal value, BigDecimal feeAmount) {
                if (kind == Kind.PERCENTAGE) {
                        return feeAmount
                                        .multiply(value)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                return value;
        }

        public record DiscountRecord(String schoolId, Kind kind, String name, BigDecimal value,
                        LocalDate expiryDate, String studentId, String feeId, String reason) {
        }
}
