package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;
import com.moriba.skultem.infrastructure.persistence.entity.PaymentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentLedgerEntryEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface StudentLedgerEntryJpaRepository
                extends JpaRepository<StudentLedgerEntryEntity, String>,
                JpaSpecificationExecutor<StudentLedgerEntryEntity> {

        Optional<StudentLedgerEntryEntity> findTopByStudentIdAndSchoolIdOrderByPaidAtDesc(String studentId,
                        String schoolId);

        Page<StudentLedgerEntryEntity> findAllByStudentIdAndSchoolId(String studentId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllByStudentIdAndAcademicYearIdAndSchoolId(String studentId,
                        String acadmicYearId, String schoolId, Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllByAcademicYearIdAndSchoolId(String acadmicYearId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllBySchoolIdOrderByPaidAtDesc(String schoolId, Pageable pageable);

        List<StudentLedgerEntryEntity> findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(String studentId,
                        String schoolId);

        /**
         * Entries that belong to the platform fee (FeeStructure#system): a fee assignment points at the
         * fee structure through its referenceId, a payment at the payment - so an entry is the
         * platform's when its reference is a platform fee structure or a payment made against one.
         * Sub-queries rather than id lists, so a school with years of payments never hits the
         * database's bind-parameter limit.
         */
        static Specification<StudentLedgerEntryEntity> platformFeeEntries(String schoolId) {
                return (root, query, cb) -> {
                        var fees = query.subquery(String.class);
                        var fee = fees.from(FeeStructureEntity.class);
                        fees.select(fee.get("id"))
                                        .where(cb.equal(fee.get("schoolId"), schoolId), cb.isTrue(fee.get("system")));

                        var payments = query.subquery(String.class);
                        var payment = payments.from(PaymentEntity.class);
                        payments.select(payment.get("id"))
                                        .where(cb.equal(payment.get("schoolId"), schoolId),
                                                        cb.isTrue(payment.get("fee").get("system")));

                        return cb.or(root.get("referenceId").in(fees), root.get("referenceId").in(payments));
                };
        }

        /** Everything else - an entry with no reference at all (an adjustment) is the school's, too. */
        static Specification<StudentLedgerEntryEntity> schoolFeeEntries(String schoolId) {
                return (root, query, cb) -> cb.or(
                                cb.isNull(root.get("referenceId")),
                                cb.not(platformFeeEntries(schoolId).toPredicate(root, query, cb)));
        }

        private static Specification<StudentLedgerEntryEntity> inYear(String academicYearId, String schoolId) {
                return (root, query, cb) -> cb.and(
                                cb.equal(root.get("schoolId"), schoolId),
                                cb.equal(root.get("academicYearId"), academicYearId));
        }

        /**
         * Ledger entries for a year narrowed by whichever of these are given: the student's name or
         * admission number, the class they're enrolled in that year, the entry's type, and its term.
         * Student and class are matched through sub-queries, so nothing is loaded into memory or
         * passed as a long id list. Says nothing about school vs platform fee - callers add that.
         */
        private static Specification<StudentLedgerEntryEntity> narrowedBy(String academicYearId, String schoolId,
                        String search, String classId, TransactionType type, String termId) {
                Specification<StudentLedgerEntryEntity> spec = inYear(academicYearId, schoolId);

                if (search != null && !search.isBlank()) {
                        var like = "%" + search.trim().toLowerCase() + "%";
                        spec = spec.and((root, query, cb) -> {
                                var students = query.subquery(String.class);
                                var student = students.from(StudentEntity.class);
                                var fullName = cb.lower(cb.concat(cb.concat(student.<String>get("givenNames"), " "),
                                                student.<String>get("familyName")));
                                students.select(student.get("id")).where(
                                                cb.equal(student.get("schoolId"), schoolId),
                                                cb.or(cb.like(fullName, like),
                                                                cb.like(cb.lower(student.<String>get("admissionNumber")),
                                                                                like)));
                                return root.get("studentId").in(students);
                        });
                }

                if (classId != null && !classId.isBlank()) {
                        spec = spec.and((root, query, cb) -> {
                                var enrolled = query.subquery(String.class);
                                var enrollment = enrolled.from(EnrollmentEntity.class);
                                enrolled.select(enrollment.get("student").get("id")).where(
                                                cb.equal(enrollment.get("schoolId"), schoolId),
                                                cb.equal(enrollment.get("clazz").get("id"), classId),
                                                cb.equal(enrollment.get("academicYear").get("id"), academicYearId));
                                return root.get("studentId").in(enrolled);
                        });
                }

                if (type != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("transactionType"), type));
                }

                if (termId != null && !termId.isBlank()) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("termId"), termId));
                }

                return spec;
        }

        /** The school's own entries (platform fee left out), narrowed - see {@link #narrowedBy}. */
        default Page<StudentLedgerEntryEntity> searchSchoolEntries(String academicYearId, String schoolId,
                        String search, String classId, TransactionType type, String termId, Pageable pageable) {
                return findAll(narrowedBy(academicYearId, schoolId, search, classId, type, termId)
                                .and(schoolFeeEntries(schoolId)), pageable);
        }

        /** Only the platform fee's entries, narrowed the same way - see {@link #narrowedBy}. */
        default Page<StudentLedgerEntryEntity> searchPlatformEntries(String academicYearId, String schoolId,
                        String search, String classId, TransactionType type, String termId, Pageable pageable) {
                return findAll(narrowedBy(academicYearId, schoolId, search, classId, type, termId)
                                .and(platformFeeEntries(schoolId)), pageable);
        }

        default Page<StudentLedgerEntryEntity> findSchoolEntries(String academicYearId, String schoolId,
                        Pageable pageable) {
                return findAll(inYear(academicYearId, schoolId).and(schoolFeeEntries(schoolId)), pageable);
        }

        default Page<StudentLedgerEntryEntity> findPlatformEntries(String academicYearId, String schoolId,
                        Pageable pageable) {
                return findAll(inYear(academicYearId, schoolId).and(platformFeeEntries(schoolId)), pageable);
        }

        default Page<StudentLedgerEntryEntity> runSchoolReport(String schoolId, List<Filter> filters,
                        Pageable pageable) {
                Specification<StudentLedgerEntryEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"),
                                schoolId);
                spec = spec.and(schoolFeeEntries(schoolId));

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }

        default Page<StudentLedgerEntryEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<StudentLedgerEntryEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }
}
