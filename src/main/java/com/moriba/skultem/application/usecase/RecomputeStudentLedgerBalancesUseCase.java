package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * One-time correction for {@link CreateStudentLedgerUsercase}'s running-balance bug: every ledger
 * entry written before that fix has its {@code balance} computed off the whole school's latest
 * entry instead of the student's own, so stored balances for schools with more than one student's
 * entries interleaved are wrong. This walks every student's entries oldest-first and recomputes
 * {@code balance} from scratch, purely from each entry's own direction/amount - it changes no
 * amounts, directions, or any other financial fact, only the running total stored alongside them.
 * <p>
 * Safe to re-run - recomputing from the same entries always lands on the same balances.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RecomputeStudentLedgerBalancesUseCase {

    private final StudentRepository studentRepo;
    private final StudentLedgerEntryRepository ledgerRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_LEDGER_BALANCES_RECOMPUTED")
    public Result execute(String schoolId) {
        var students = studentRepo.findBySchoolId(schoolId, Pageable.unpaged()).getContent();

        int studentsAffected = 0;
        int entriesUpdated = 0;

        for (var student : students) {
            var entries = ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(student.getId(),
                    schoolId);

            if (entries.isEmpty()) {
                continue;
            }

            List<StudentLedgerEntry> changed = recomputeBalances(entries);

            if (!changed.isEmpty()) {
                ledgerRepo.saveAll(changed);
                studentsAffected++;
                entriesUpdated += changed.size();
            }
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Student ledger balances recomputed",
                studentsAffected + " student(s), " + entriesUpdated + " entrie(s) corrected",
                null,
                null);

        return new Result(students.size(), studentsAffected, entriesUpdated);
    }

    private List<StudentLedgerEntry> recomputeBalances(List<StudentLedgerEntry> entriesOldestFirst) {
        List<StudentLedgerEntry> changed = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;

        for (var entry : entriesOldestFirst) {
            BigDecimal amount = entry.getAmount() != null ? entry.getAmount() : BigDecimal.ZERO;
            running = entry.getDirection() == StudentLedgerEntry.Direction.DEBIT
                    ? running.add(amount)
                    : running.subtract(amount);

            if (entry.getBalance() == null || entry.getBalance().compareTo(running) != 0) {
                entry.recalculateBalance(running);
                changed.add(entry);
            }
        }

        return changed;
    }

    public record Result(int totalStudents, int studentsCorrected, int entriesCorrected) {
    }
}
