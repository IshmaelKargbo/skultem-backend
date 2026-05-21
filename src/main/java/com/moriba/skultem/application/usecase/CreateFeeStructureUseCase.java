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
import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.FeeStructure.Type;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.TermRepository;
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
        private final MaterialRepository materialRepo;
        private final FeeStructureRepository repo;
        private final EnrollmentRepository enrollmentRepo;
        private final StudentFeeRepository studentFeeRepo;
        private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
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

                Material material = null;

                if (param.hasSuppy && param.materialId != null) {
                        material = materialRepo.findByIdAndSchool(param.materialId, param.schoolId).orElseThrow(() -> new NotFoundException("material not found"));
                }

                var fee = FeeStructure.create(
                                param.schoolId(),
                                param.type(),
                                clazz,
                                param.hasSuppy(),
                                param.totalSupply(),
                                term,
                                category,
                                material,
                                academicYear,
                                param.dueDate(),
                                param.amount(),
                                param.description(),
                                param.allowInstallment());

                repo.save(fee);

                List<Enrollment> enrollments;

                if (param.studentIds() != null && !param.studentIds().isEmpty()) {
                        enrollments = enrollmentRepo.findAllByStudentIdsAndAcademicYearAndSchoolId(
                                        param.studentIds(),
                                        academicYear.getId(),
                                        param.schoolId());

                } else if (clazz != null) {

                        enrollments = enrollmentRepo.findAllByClassAndAcademicAndSchoolId(
                                        clazz.getId(),
                                        academicYear.getId(),
                                        param.schoolId(),
                                        Pageable.unpaged())
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
                                        enrollment.getId(), enrollment.getStudent().getId(), fee.getId())) {
                                continue;
                        }

                        var studentFee = StudentFee.create(
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

                String target = clazz != null
                                ? clazz.getName()
                                : param.studentIds() != null && !param.studentIds().isEmpty()
                                                ? "Selected students"
                                                : "All classes";

                String meta = "assignedCount=" + assignedCount
                                + ";targetCount=" + enrollments.size()
                                + ";totalAmount=" + totalAssignedAmount;

                logActivityUseCase.log(
                                param.schoolId(),
                                ActivityType.FEES,
                                "Fee structure created",
                                category.getName() + " - " + term.getName() + " - " + target,
                                meta,
                                fee.getId());

                return FeeStructureMapper.toDTO(fee);
        }

        public record StructureRecord(
                        String schoolId,
                        Type type,
                        String classId,
                        List<String> studentIds,
                        String feeCategory,
                        String termId,
                        String materialId,
                        BigDecimal amount,
                        LocalDate dueDate,
                        boolean allowInstallment,
                        String description,
                        boolean hasSuppy,
                        int totalSupply) {
        }
}