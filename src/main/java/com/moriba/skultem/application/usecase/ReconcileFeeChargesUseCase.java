package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeChargePurgeRepository;
import com.moriba.skultem.domain.repository.FeeChargePurgeRepository.Charge;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Brings students' accounts in line with a fee structure after it was edited (see
 * {@link UpdateFeeStructureUseCase}, and the platform fee via {@link UpdatePlatformFeeSettingUseCase}).
 * <ul>
 * <li>Every student charged it gets the new amount, term and wording on their ledger charge, and any discount
 * on it is worked out again (a percentage follows the new amount; a fixed discount is capped at what's still
 * owed).</li>
 * <li>When who it applies to changed (new/old students only, gender, term), students who no longer qualify
 * have the charge taken off, and students who now qualify are charged - the same rules a new fee uses.</li>
 * <li>Running balances are recomputed for every student touched.</li>
 * </ul>
 * Refused (nothing changes) when a student has already paid more than the new amount, or has paid towards a
 * charge that would be taken off - that money has to stay on record.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReconcileFeeChargesUseCase {

    private final FeeChargePurgeRepository chargeRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final FeeDiscountRepository discountRepo;
    private final StudentLedgerEntryRepository ledgerRepo;
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final RecomputeStudentLedgerBalancesUseCase recomputeBalances;

    public record Result(int updated, int added, int removed) {
        public boolean changedAnything() {
            return updated + added + removed > 0;
        }
    }

    public Result execute(FeeStructure fee, boolean rulesChanged) {
        String schoolId = fee.getSchoolId();
        String feeId = fee.getId();
        BigDecimal amount = fee.getAmount() == null ? BigDecimal.ZERO : fee.getAmount();

        List<Charge> charges = chargeRepo.charges(schoolId, feeId);
        Map<String, Enrollment> enrollments = enrollmentRepo
                .findAllByAcademicSchoolId(fee.getAcademicYear().getId(), schoolId).stream()
                .collect(Collectors.toMap(Enrollment::getId, Function.identity(), (a, b) -> a));

        // Who should carry it now. Only for fees reaching a whole class/school by rule - a hand-picked selection
        // (and the platform fee) keeps exactly the students it has.
        List<Charge> toRemove = new ArrayList<>();
        List<Enrollment> toAdd = new ArrayList<>();
        if (rulesChanged && !fee.isSystem() && fee.getType() != FeeStructure.Type.SELECTION) {
            Set<String> charged = charges.stream().map(Charge::enrollmentId).collect(Collectors.toSet());
            Set<String> qualifying = new HashSet<>();
            for (Enrollment e : enrollments.values()) {
                if (inScope(fee, e) && qualifies(fee, e)) {
                    qualifying.add(e.getId());
                    if (!charged.contains(e.getId()) && isActive(e.getStudent())) {
                        toAdd.add(e);
                    }
                }
            }
            charges.stream().filter(c -> !qualifying.contains(c.enrollmentId())).forEach(toRemove::add);
        }

        long paidButRemoved = toRemove.stream().filter(c -> c.paid().signum() > 0).count();
        if (paidButRemoved > 0) {
            throw new RuleException(paidButRemoved + " student(s) who would no longer be charged this fee have "
                    + "already paid towards it. Record a refund first, or keep who it applies to as it was.");
        }

        Set<String> removedStudents = toRemove.stream().map(Charge::studentId).collect(Collectors.toSet());
        List<Charge> kept = charges.stream().filter(c -> !removedStudents.contains(c.studentId())).toList();

        BigDecimal maxPaid = kept.stream().map(Charge::paid).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        if (maxPaid.compareTo(amount) > 0) {
            long over = kept.stream().filter(c -> c.paid().compareTo(amount) > 0).count();
            throw new RuleException(over + " student(s) have already paid more than " + amount
                    + " towards this fee (up to " + maxPaid + "). Set the amount to at least " + maxPaid + ".");
        }

        if (!toRemove.isEmpty()) {
            chargeRepo.removeCharges(schoolId, feeId, new ArrayList<>(removedStudents));
        }

        // Existing charges: amount, term and wording; discounts worked out against the new amount.
        int updated = 0;
        String termId = fee.getTerm().getId();
        String termName = fee.getTerm().getName();
        String category = fee.getCategory().getName();
        for (Charge c : kept) {
            Student student = studentOf(c, enrollments);
            List<StudentLedgerEntry> changed = new ArrayList<>();
            Map<String, FeeDiscount> discounts = discountRepo.findBySchoolAndStudentIdAndFeeId(schoolId,
                    c.studentId(), feeId).stream().collect(Collectors.toMap(FeeDiscount::getId, d -> d, (a, b) -> a));

            for (StudentLedgerEntry entry : ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(
                    c.studentId(), schoolId)) {
                if (entry.getTransactionType() == TransactionType.FEE_ASSINMENT && feeId.equals(entry.getReferenceId())) {
                    String desc = describe(TransactionType.FEE_ASSINMENT, termName, category, student, amount,
                            entry.getDescription());
                    if (differs(entry, termId, amount, desc)) {
                        entry.reassignPayment(termId, amount, desc);
                        changed.add(entry);
                    }
                } else if (entry.getTransactionType() == TransactionType.DISCOUNT
                        && discounts.containsKey(entry.getReferenceId())) {
                    BigDecimal off = discountAmount(discounts.get(entry.getReferenceId()), amount, c.paid());
                    String desc = describe(TransactionType.DISCOUNT, termName, category, student, off,
                            entry.getDescription());
                    if (differs(entry, termId, off, desc)) {
                        entry.reassignPayment(termId, off, desc);
                        changed.add(entry);
                    }
                }
            }

            if (!changed.isEmpty()) {
                ledgerRepo.saveAll(changed);
                recomputeBalances.recomputeForStudent(c.studentId(), schoolId);
                updated++;
            }
        }

        removedStudents.forEach(studentId -> recomputeBalances.recomputeForStudent(studentId, schoolId));

        // Newly qualifying students: charged the same way a new fee charges them.
        for (Enrollment e : toAdd) {
            Student student = e.getStudent();
            studentFeeRepo.save(StudentFee.create(schoolId, e, student, fee, null));
            createStudentLedgerUsercase.createEntry(schoolId, fee.getAcademicYear().getId(), student.getId(), termId,
                    TransactionType.FEE_ASSINMENT, Direction.DEBIT, amount, feeId,
                    describe(TransactionType.FEE_ASSINMENT, termName, category, student, amount, null),
                    Instant.now());
        }

        return new Result(updated, toAdd.size(), removedStudents.size());
    }

    // ALL reaches every class; CLASS only its class (and stream, when the fee names one).
    private static boolean inScope(FeeStructure fee, Enrollment e) {
        if (fee.getType() != FeeStructure.Type.CLASS) {
            return true;
        }
        if (fee.getClazz() == null || e.getClazz() == null || !fee.getClazz().getId().equals(e.getClazz().getId())) {
            return false;
        }
        return fee.getStream() == null
                || (e.getStream() != null && fee.getStream().getId().equals(e.getStream().getId()));
    }

    // Same rules as CreateFeeStructureUseCase / ApplyApplicableFeesToEnrollmentUseCase.
    private static boolean qualifies(FeeStructure fee, Enrollment e) {
        Student s = e.getStudent();
        if (s == null) {
            return false;
        }
        if (!fee.appliesToEnrollmentType(s.getEnrollmentType())) {
            return false;
        }
        if (fee.getGender() != null && fee.getGender() != s.getGender()) {
            return false;
        }
        var template = e.getClazz() == null ? null : e.getClazz().getTemplate();
        return template == null || fee.getTerm() == null || fee.getTerm().getTermNumber() >= template.getStartsInTerm();
    }

    private static boolean isActive(Student s) {
        return s != null && s.getStatus() == Student.Status.ACTIVE;
    }

    private static Student studentOf(Charge c, Map<String, Enrollment> enrollments) {
        Enrollment e = enrollments.get(c.enrollmentId());
        return e == null ? null : e.getStudent();
    }

    // A percentage follows the fee; a fixed discount stays as it was. Either way it can't take off more than is
    // still owed on the fee, so the student's balance never goes below zero.
    private static BigDecimal discountAmount(FeeDiscount d, BigDecimal feeAmount, BigDecimal paid) {
        BigDecimal value = d.getValue() == null ? BigDecimal.ZERO : d.getValue();
        BigDecimal off = d.getKind() == FeeDiscount.Kind.PERCENTAGE
                ? feeAmount.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value;
        BigDecimal owed = feeAmount.subtract(paid).max(BigDecimal.ZERO);
        return off.min(owed);
    }

    // Without the student (not enrolled this year any more) the existing wording is kept.
    private static String describe(TransactionType type, String term, String category, Student s, BigDecimal amount,
            String current) {
        if (s == null) {
            return current;
        }
        return Generate.generateLedgerDescription(type, term, category, s.getGivenNames(), s.getFamilyName(),
                s.getAdmissionNumber(), amount);
    }

    private static boolean differs(StudentLedgerEntry entry, String termId, BigDecimal amount, String desc) {
        return !Objects.equals(entry.getTermId(), termId)
                || entry.getAmount() == null || entry.getAmount().compareTo(amount) != 0
                || !Objects.equals(entry.getDescription(), desc);
    }
}
