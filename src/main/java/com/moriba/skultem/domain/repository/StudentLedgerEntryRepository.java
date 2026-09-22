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

        void deleteAll(List<StudentLedgerEntry> domains);

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

        /**
         * The school's own ledger entries for a year - everything except the platform fee (see
         * FeeStructure#system), which is Skultem's and lives on its own page.
         */
        Page<StudentLedgerEntry> findSchoolEntries(String academicYearId, String schoolId, Pageable pageable);

        /**
         * {@link #findSchoolEntries} narrowed by any of: the student's name or admission number, the
         * class they're enrolled in, the entry type, and its term - each ignored when null/blank.
         */
        Page<StudentLedgerEntry> searchSchoolEntries(String academicYearId, String schoolId, String search,
                        String classId, StudentLedgerEntry.TransactionType type, String termId, Pageable pageable);

        /** {@link #searchSchoolEntries} for the platform fee's entries instead of the school's own. */
        Page<StudentLedgerEntry> searchPlatformEntries(String academicYearId, String schoolId, String search,
                        String classId, StudentLedgerEntry.TransactionType type, String termId, Pageable pageable);

        /** Only the platform fee's entries for a year: the fee charged to students and payments towards it. */
        Page<StudentLedgerEntry> findPlatformEntries(String academicYearId, String schoolId, Pageable pageable);

        Page<StudentLedgerEntry> findAllByStudentAndSchool(String studentId, String schoolId, Pageable pageable);

        Page<StudentLedgerEntry> runReport(String schoolId, List<Filter> filters, Pageable pageable);

        /** {@link #runReport} for the school's own fees only - the platform fee's entries left out. */
        Page<StudentLedgerEntry> runSchoolReport(String schoolId, List<Filter> filters, Pageable pageable);

    /** Wipes every row for this school - see WipeTestSchoolDataUseCase. */
    void deleteAllBySchoolId(String schoolId);
}
