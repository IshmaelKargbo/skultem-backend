package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.vo.Filter;

public interface StudentLedgerEntryRepository {
        void save(StudentLedgerEntry domain);

        void saveAll(List<StudentLedgerEntry> domains);

        /**
         * Every ledger entry for one student, oldest first (tie-broken by creation order) - the exact
         * order their running balance was meant to accumulate in. Used to recompute
         * {@link StudentLedgerEntry#recalculateBalance} from scratch.
         */
        List<StudentLedgerEntry> findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(String studentId,
                        String schoolId);

        /**
         * Most recent ledger entry for one student - the running balance a new entry for them builds
         * on. Scoped by student, not just school: a student's balance must never be computed off some
         * other student's latest entry.
         */
        Optional<StudentLedgerEntry> findTopByStudentIdAndSchoolIdOrderByPaidAtDesc(String studentId, String schoolId);

        Page<StudentLedgerEntry> findAllBySchoolIdOrderByPaidAtDesc(String schoolId, Pageable pageable);

        Page<StudentLedgerEntry> findAllByAcademicYearAndStudentAndSchool(String academicYearId, String studentId,
                        String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntry> findAllByAcademicYearAndSchool(String academicYearId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntry> findAllByStudentAndSchool(String studentId, String schoolId, Pageable pageable);

        Page<StudentLedgerEntry> runReport(String schoolId, List<Filter> filters, Pageable pageable);

}
