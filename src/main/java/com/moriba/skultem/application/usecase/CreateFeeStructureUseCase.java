package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.FeeStructureSupplyItem;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.FeeStructure.Type;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.infrastructure.rest.dto.FeeStructureSupplyItemInputDTO;
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

        // A CLASS-type fee can target several classes at once (see CreateFeeStructureDTO.classIds) -
        // each one still becomes its own independent FeeStructure row, created and backfilled exactly
        // as it always was for a single class, just looped. ALL/SELECTION never had a class to loop
        // over, so they run the same single pass they always did (one iteration, classId null).
        @AuditLogAnnotation(action = "FEE_STRUCTURE_CREATED")
        public List<FeeStructureDTO> execute(StructureRecord param) {
                var term = termRepo.findByIdAndSchoolId(param.termId(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Term not found"));

                var academicYear = term.getAcademicYear();

                if (academicYear.isLocked()) {
                        throw new IllegalStateException("Cannot create a fee structure in a closed academic year");
                }

                var category = feeCategoryRepo.findByIdAndSchool(param.feeCategory(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Fee category not found"));

                // Resolved once, then re-materialized with a fresh id per class below (createForClass) -
                // each class gets its own independent FeeStructure, so its supply items need their own
                // ids too, not one shared set of item objects reused across every row.
                List<ResolvedSupplyItem> resolvedSupplyItems = new ArrayList<>();
                if (param.supplyItems() != null) {
                        for (var item : param.supplyItems()) {
                                var material = materialRepo.findByIdAndSchool(item.materialId(), param.schoolId())
                                                .orElseThrow(() -> new NotFoundException("Material not found"));
                                resolvedSupplyItems.add(new ResolvedSupplyItem(material, item.quantity()));
                        }
                }

                List<String> classIds = param.classIds() != null && !param.classIds().isEmpty()
                                ? param.classIds()
                                : java.util.Collections.singletonList(null);

                List<FeeStructureDTO> created = new ArrayList<>();

                for (String classId : classIds) {
                        created.add(createForClass(param, term, academicYear, category, resolvedSupplyItems, classId));
                }

                return created;
        }

        private FeeStructureDTO createForClass(StructureRecord param, Term term, AcademicYear academicYear,
                        FeeCategory category, List<ResolvedSupplyItem> resolvedSupplyItems, String classId) {

                var clazz = classId != null
                                ? classRepo.findByIdAndSchool(classId, param.schoolId())
                                                .orElseThrow(() -> new NotFoundException("Class not found"))
                                : null;

                // Only guards CLASS-type fees - the underlying query compares clazz.id, which never
                // matches a null clazz (ALL/SELECTION), so it can't reliably catch a duplicate there.
                // A plain fee (neither flag set, no gender) can't coexist with anything else for the
                // same class/term/category, but newStudentsOnly/oldStudentsOnly and different genders
                // are each a deliberate partition allowed to coexist - see existsOverlappingFeeStructure.
                if (clazz != null && repo.existsOverlappingFeeStructure(param.schoolId(), academicYear.getId(),
                                term.getId(), clazz.getId(), category.getId(), param.newStudentsOnly(),
                                param.oldStudentsOnly(), param.gender())) {
                        throw new AlreadyExistsException(category.getName() + " already has a fee structure for "
                                        + clazz.getName() + " in " + term.getName()
                                        + (param.newStudentsOnly() || param.oldStudentsOnly() || param.gender() != null
                                                        ? " that overlaps this one"
                                                        : ""));
                }

                List<FeeStructureSupplyItem> supplyItems = resolvedSupplyItems.stream()
                                .map(r -> new FeeStructureSupplyItem(UUID.randomUUID().toString(), r.material(),
                                                r.quantity()))
                                .toList();

                var fee = FeeStructure.create(
                                param.schoolId(),
                                param.type(),
                                clazz,
                                param.hasSuppy(),
                                supplyItems,
                                term,
                                category,
                                academicYear,
                                param.dueDate(),
                                param.amount(),
                                param.description(),
                                param.allowInstallment(),
                                param.newStudentsOnly(),
                                param.oldStudentsOnly(),
                                param.gender());

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

                // Same "not for an explicit selection" rule as newStudentsOnly/oldStudentsOnly above
                // (and enforced the same way at the DTO level) - a hand-picked list of students is
                // already a deliberate, one-off assignment, so a gender filter would only add a
                // confusing silent exclusion rather than anything useful.
                if (param.gender() != null && !hasExplicitStudents) {
                        enrollments = enrollments.stream()
                                        .filter(e -> e.getStudent().getGender() == param.gender())
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

        private record ResolvedSupplyItem(com.moriba.skultem.domain.model.Material material, int quantity) {
        }

        public record StructureRecord(
                        String schoolId,
                        Type type,
                        List<String> classIds,
                        List<String> studentIds,
                        String feeCategory,
                        String termId,
                        BigDecimal amount,
                        LocalDate dueDate,
                        boolean allowInstallment,
                        String description,
                        boolean hasSuppy,
                        List<FeeStructureSupplyItemInputDTO> supplyItems,
                        boolean newStudentsOnly,
                        boolean oldStudentsOnly,
                        Gender gender) {
        }
}
