package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.*;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateFeeStructureUseCase {

        private final FeeCategoryRepository feeCategoryRepo;
        private final ClassRepository classRepo;
        private final AcademicYearRepository academicYearRepo;
        private final TermRepository termRepo;
        private final FeeStructureRepository repo;
        private final EnrollmentRepository enrollmentRepo;
        private final StudentFeeRepository studentFeeRepo;
        private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
        private final ReferenceGeneratorUsecase rg;
        private final LogActivityUseCase logActivityUseCase;

        @AuditLogAnnotation(action = "FEE_STRUCTURE_CREATED")
        public FeeStructureDTO execute(StructureRecord param) {
                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                var category = feeCategoryRepo.findByIdAndSchool(param.feeCategory(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Fee category not found"));

                var term = termRepo.findByIdAndAcademicYearIdAndSchoolId(
                                param.termId(),
                                academicYear.getId(),
                                param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Term not found"));

                var clazz = param.classId() != null
                                ? classRepo.findByIdAndSchool(param.classId(), param.schoolId())
                                                .orElseThrow(() -> new NotFoundException("Class not found"))
                                : null;

                var id = rg.generate("FEE_STRUCTURE", "FES");

                var fee = FeeStructure.create(
                                id,
                                param.schoolId(),
                                clazz,
                                term,
                                category,
                                academicYear,
                                param.dueDate(),
                                param.amount(),
                                param.description(),
                                param.allowInstallment());

                repo.save(fee);

                List<Enrollment> enrollments;

                if (clazz != null) {
                        enrollments = enrollmentRepo.findAllByClassAndAcademicSchoolId(
                                        clazz.getId(),
                                        academicYear.getId(),
                                        param.schoolId(), Pageable.unpaged())
                                        .getContent();
                } else {
                        enrollments = enrollmentRepo.findAllByAcademicSchoolId(
                                        academicYear.getId(),
                                        param.schoolId());
                }

                int assignedCount = 0;
                BigDecimal totalAssignedAmount = BigDecimal.ZERO;

                for (Enrollment enrollment : enrollments) {
                        if (studentFeeRepo.existsBySchoolAndEnrollmentAndStudentAndFee(param.schoolId(),
                                        enrollment.getId(), enrollment.getStudent().getId(), id)) {
                                continue;
                        }

                        var studentFee = StudentFee.create(
                                        rg.generate("STUDENT_FEE", "STF"),
                                        param.schoolId(),
                                        enrollment,
                                        enrollment.getStudent(),
                                        fee,
                                        null);

                        studentFeeRepo.save(studentFee);

                        var description = Generate.generateLedgerDescription(
                                        TransactionType.FEE_ASSINMENT,
                                        term.getName(),
                                        category.getName(),
                                        enrollment.getStudent().getGivenNames(),
                                        enrollment.getStudent().getFamilyName(),
                                        enrollment.getStudent().getAdmissionNumber(),
                                        param.amount());

                        createStudentLedgerUsercase.createEntry(
                                        param.schoolId(),
                                        academicYear.getId(),
                                        enrollment.getStudent().getId(),
                                        term.getId(),
                                        TransactionType.FEE_ASSINMENT,
                                        Direction.DEBIT,
                                        param.amount(),
                                        fee.getId(),
                                        description,
                                        Instant.now());

                        assignedCount += 1;
                        totalAssignedAmount = totalAssignedAmount.add(param.amount());
                }

                String className = clazz != null ? clazz.getName() : "All classes";
                String meta = "assignedCount=" + assignedCount
                                + ";targetCount=" + enrollments.size()
                                + ";totalAmount=" + totalAssignedAmount;
                logActivityUseCase.log(
                                param.schoolId(),
                                ActivityType.FEES,
                                "Fee structure created",
                                category.getName() + " - " + term.getName() + " - " + className,
                                meta,
                                fee.getId());

                return FeeStructureMapper.toDTO(fee);
        }

        public record StructureRecord(
                        String schoolId,
                        String classId,
                        String feeCategory,
                        String termId,
                        BigDecimal amount,
                        LocalDate dueDate,
                        boolean allowInstallment,
                        String description) {
        }
}
