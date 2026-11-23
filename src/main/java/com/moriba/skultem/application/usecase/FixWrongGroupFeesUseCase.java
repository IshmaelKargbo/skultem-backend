package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.FeeChargePurgeRepository;
import com.moriba.skultem.domain.repository.FeeChargePurgeRepository.WrongGroupCharge;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Finds and takes off fees charged to the wrong group of students: a "new students only" fee on an old student
 * (RE_ENROLLMENT or EXISTING), or an "old students only" fee on a new one (NEW or TRANSFER) - which is how a student
 * ends up owing both the new and the old student fee. The fee that doesn't match the student's enrollment type is
 * the one taken off; if the type itself is wrong, the school corrects it afterwards and the fees follow
 * ({@link ChangeStudentEnrollmentTypeUseCase}).
 * <p>
 * Only open academic years are touched. A charge with a payment or a discount against it is never removed here -
 * it is listed as needing a look, so no money disappears from the record.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FixWrongGroupFeesUseCase {

    private final FeeChargePurgeRepository chargeRepo;
    private final RecomputeStudentLedgerBalancesUseCase recomputeBalances;
    private final LogActivityUseCase logActivityUseCase;

    public record Item(String studentId, String studentName, String admissionNumber, String enrollmentType,
            String className, String feeId, String feeName, String termName, String feeFor, BigDecimal amount,
            BigDecimal paid, boolean discounted, boolean canRemove, String reason) {
    }

    public record Result(int removed, int needsAttention, List<Item> items) {
    }

    /** What would be taken off, without changing anything. */
    public Result preview(String schoolId) {
        List<Item> items = items(schoolId);
        int removable = (int) items.stream().filter(Item::canRemove).count();
        return new Result(removable, items.size() - removable, items);
    }

    /** Takes off every wrong-group charge that has no payment or discount; lists the rest. */
    public Result fix(String schoolId) {
        List<Item> items = items(schoolId);

        Map<String, Set<String>> studentsByFee = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.canRemove()) {
                studentsByFee.computeIfAbsent(item.feeId(), k -> new LinkedHashSet<>()).add(item.studentId());
            }
        }
        Set<String> touched = new LinkedHashSet<>();
        studentsByFee.forEach((feeId, students) -> {
            chargeRepo.removeCharges(schoolId, feeId, List.copyOf(students));
            touched.addAll(students);
        });
        touched.forEach(studentId -> recomputeBalances.recomputeForStudent(studentId, schoolId));

        int removed = (int) items.stream().filter(Item::canRemove).count();
        if (removed > 0) {
            logActivityUseCase.log(schoolId, ActivityType.FEES, "Wrong new/old student fees removed",
                    removed + " charge(s) across " + touched.size() + " student(s)",
                    "removed=" + removed + ";students=" + touched.size(), null);
        }
        return new Result(removed, items.size() - removed, items);
    }

    private List<Item> items(String schoolId) {
        return chargeRepo.wrongGroupCharges(schoolId).stream().map(FixWrongGroupFeesUseCase::item).toList();
    }

    private static Item item(WrongGroupCharge c) {
        boolean paid = c.paid() != null && c.paid().signum() > 0;
        String reason = paid ? "Has payments - record a refund or move the payment first"
                : c.discounted() ? "Has a discount - remove the discount first" : null;
        return new Item(c.studentId(), c.studentName(), c.admissionNumber(), c.enrollmentType(), c.className(),
                c.feeId(), c.feeName(), c.termName(), c.newStudentsOnly() ? "New students only" : "Old students only",
                c.amount(), c.paid(), c.discounted(), reason == null, reason);
    }
}
