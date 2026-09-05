package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.FeeStructure.Type;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
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
        private final TermRepository termRepo;
        private final MaterialRepository materialRepo;
        private final FeeStructureRepository repo;
        private final EnrollmentRepository enrollmentRepo;
        private final StudentFeeRepository studentFeeRepo;
        private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
        private final LogActivityUseCase logActivityUseCase;

        @AuditLogAnnotation(action = "FEE_STRUCTURE_CREATED")
        public FeeStructureDTO execute(StructureRecord param) {

                // The academic year comes from the term itself, not "whichever year is currently
                // active" - a school preparing an upcoming year's fee structures ahead of time
                // (before activating it) is a normal workflow, same as ApplyApplicableFeesToEnrollmentUseCase
                // already treating academic year as a property of what it's touching rather than
                // requiring it to be the active one.
                var term = termRepo.findByIdAndSchoolId(param.termId(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Term not found"));

                var academicYear = term.getAcademicYear();

                if (academicYear.isLocked()) {
                        throw new IllegalStateException("Cannot create a fee structure in a closed academic year");
                }

                var category = feeCategoryRepo.findByIdAndSchool(param.feeCategory(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Fee category not found"));

                var clazz = param.classId() != null
                                ? classRepo.findByIdAndSchool(param.classId(), param.schoolId())
                                                .orElseThrow(() -> new NotFoundException("Class not found"))
                                : null;

                // Only guards CLASS-type fees - the underlying query compares clazz.id, which never
                // matches a null clazz (ALL/SELECTION), so it can't reliably catch a duplicate there.
                // A plain fee (neither flag set) can't coexist with anything else for the same
                // class/term/category, but a newStudentsOnly and an oldStudentsOnly fee are allowed
                // to coexist - that's how a school charges, say, 900 Tuition for new students and 700
                // for old/returning students in the same class - see existsOverlappingFeeStructure.
                if (clazz != null && repo.existsOverlappingFeeStructure(param.schoolId(), academicYear.getId(),
                                term.getId(), clazz.getId(), category.getId(), param.newStudentsOnly(),
                                param.oldStudentsOnly())) {
                        throw new AlreadyExistsException(category.getName() + " already has a fee structure for "
                                        + clazz.getName() + " in " + term.getName()
                                        + (param.newStudentsOnly() || param.oldStudentsOnly()
                                                        ? " that overlaps this one"
                                                        : ""));
                }

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
                                param.allowInstallment(),
                                param.newStudentsOnly(),
                                param.oldStudentsOnly());

                repo.save(fee);

                List<Enrollment> enrollments;
                boolean hasExplicitStudents = param.studentIds() != null && !param.studentIds().isEmpty();

                if (hasExplicitStudents) {
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

                // Only reach students whose overall admission was NEW/TRANSFER - not the explicit
                // selection above, same as ApplyApplicableFeesToEnrollmentUseCase (which is what
                // enforces this same rule for enrollments created after this fee already exists).
                if (param.newStudentsOnly() && !hasExplicitStudents) {
                        enrollments = enrollments.stream()
                                        .filter(e -> e.getStudent().getEnrollmentType() == EnrollmentType.NEW
                                                        || e.getStudent().getEnrollmentType() == EnrollmentType.TRANSFER)
                                        .toList();
                }

                // Mirror image of the above - only reach students whose overall admission was
                // RE_ENROLLMENT (a returning student), same as ApplyApplicableFeesToEnrollmentUseCase.
                if (param.oldStudentsOnly() && !hasExplicitStudents) {
                        enrollments = enrollments.stream()
                                        .filter(e -> e.getStudent().getEnrollmentType() == EnrollmentType.RE_ENROLLMENT)
                                        .toList();
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
                                : hasExplicitStudents
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
                        String material,
                        int totalSupply,
                        boolean newStudentsOnly,
                        boolean oldStudentsOnly) {
        }
}