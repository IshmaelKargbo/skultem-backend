package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.*;
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

        public FeeStructureDTO execute(StructureRecord param) {

                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId())
                                .orElseThrow(() -> new NotFoundException("academic year not found"));

                var category = feeCategoryRepo.findByIdAndSchool(param.feeCategory(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("fee category not found"));

                var term = termRepo.findByIdAndAcademicYearIdAndSchoolId(
                                param.termId, academicYear.getId(), param.schoolId)
                                .orElseThrow(() -> new NotFoundException("no term found"));

                var clazz = param.classId() != null
                                ? classRepo.findByIdAndSchool(param.classId(), param.schoolId())
                                                .orElseThrow(() -> new NotFoundException("class not found"))
                                : null;

                var id = rg.generate("FEE_STRUCTURE", "FES");

                var fee = FeeStructure.create(id, param.schoolId(), clazz, term, category, academicYear, param.dueDate,
                                param.amount, param.description, param.allowInstallment);

                repo.save(fee);

                // 🔥 Assign to students
                List<Enrollment> enrollments;

                if (param.classId() != null) {
                        enrollments = enrollmentRepo.findAllByClassAndAcademicSchoolId(param.classId(),
                                        academicYear.getId(), param.schoolId());
                } else {
                        enrollments = enrollmentRepo.findAllByAcademicSchoolId(academicYear.getId(), param.schoolId());
                }

                List<StudentFee> studentFees = enrollments.stream()
                                .map(enrollment -> StudentFee.create(rg.generate("STUDENT_FEE", "STF"), param.schoolId,
                                                enrollment, enrollment.getStudent(), fee, null))
                                .toList();

                for (StudentFee studentFee : studentFees) {
                        var description = Generate.generateLedgerDescription(TransactionType.FEE_ASSINMENT,
                                        term.getName(), fee.getCategory().getName(),
                                        studentFee.getStudent().getGivenNames(),
                                        studentFee.getStudent().getFamilyName(),
                                        studentFee.getStudent().getAdmissionNumber(), param.amount());

                        studentFeeRepo.save(studentFee);
                        createStudentLedgerUsercase.createEntry(param.schoolId(), academicYear.getId(),
                                        studentFee.getStudent().getId(), term.getId(), TransactionType.FEE_ASSINMENT,
                                        Direction.DEBIT, param.amount(), id, description, Instant.now());
                }

                return FeeStructureMapper.toDTO(fee);
        }

        public record StructureRecord(String schoolId,
                        String classId,
                        String feeCategory,
                        String termId,
                        BigDecimal amount,
                        LocalDate dueDate,
                        boolean allowInstallment,
                        String description) {
        }
}