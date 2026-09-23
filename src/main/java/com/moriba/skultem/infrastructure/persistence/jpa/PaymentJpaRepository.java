package com.moriba.skultem.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.FeeCategoryRevenue;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.PaymentEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface PaymentJpaRepository
        extends JpaRepository<PaymentEntity, String>, JpaSpecificationExecutor<PaymentEntity> {

    Page<PaymentEntity> findAllByStudent_Id(String studentId, Pageable pageable);

    // A student's payments towards fees of one academic year (a fee structure belongs to a year).
    Page<PaymentEntity> findAllByStudent_IdAndFee_AcademicYear_Id(String studentId, String academicYearId,
            Pageable pageable);

    List<PaymentEntity> findAllByReferenceNoAndSchoolIdOrderByCreatedAtAsc(String referenceNo, String schoolId);

    Page<PaymentEntity> findAllByFee_AcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(
            String academicYearId,
            String schoolId,
            Pageable pageable);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.student.id = :studentId
            and p.fee.id = :feeId
            """)
    BigDecimal sumPaymentsByStudentAndFee(String studentId, String feeId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.student.id = :studentId
            and p.fee.academicYear.id = :academicYearId
            """)
    BigDecimal sumPaymentsByStudentThisYear(String studentId, String academicYearId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.fee.id = :feeId
            and p.schoolId = :schoolId
            """)
    BigDecimal sumPaymentsByFeeAndSchool(String feeId, String schoolId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.schoolId = :schoolId
            """)
    BigDecimal sumPaymentsBySchool(String schoolId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.schoolId = :schoolId
            and p.createdAt between :start and :end
            """)
    BigDecimal sumPaymentsBySchoolAndDateRange(String schoolId, Instant start, Instant end);

    @Query("""
            SELECT new com.moriba.skultem.domain.model.FeeCategoryRevenue(
                f.category.name,
                SUM(p.amount)
            )
            FROM PaymentEntity p
            JOIN p.fee f
            WHERE p.schoolId = :schoolId
            GROUP BY f.category.name
            """)
    List<FeeCategoryRevenue> sumRevenueByCategory(String schoolId);

    default Page<PaymentEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<PaymentEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }

    // Same as sumPaymentsBySchool, but the platform fee (fee.system = true) is excluded - the one
    // the school's own "total collected" financial reports must use (see FinanceReportUseCase).
    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.schoolId = :schoolId
            and p.fee.system = false
            """)
    BigDecimal sumSchoolPaymentsBySchool(String schoolId);

    // Paid-so-far per (student, fee) pair, for the school's own fees of one academic year
    // (optionally one term) - one query for every student instead of one query per StudentFee row.
    // Row shape: [studentId, feeId, totalPaid].
    @Query("""
            select p.student.id, p.fee.id, coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.schoolId = :schoolId
            and p.fee.academicYear.id = :academicYearId
            and p.fee.system = false
            and (:termId = '' or p.fee.term.id = :termId)
            group by p.student.id, p.fee.id
            """)
    List<Object[]> sumSchoolPaymentsGroupedByStudentAndFee(@Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId, @Param("termId") String termId);

    // School-fee payments collected in [start, end), grouped by method - backs the Daily Collection
    // and Payment Method Summary reports. Row shape: [method, totalAmount, transactionCount].
    @Query("""
            select p.method, coalesce(sum(p.amount), 0), count(p)
            from PaymentEntity p
            where p.schoolId = :schoolId
            and p.fee.system = false
            and p.paidAt >= :start and p.paidAt < :end
            group by p.method
            """)
    List<Object[]> sumSchoolPaymentsByMethodAndDateRange(@Param("schoolId") String schoolId,
            @Param("start") Instant start, @Param("end") Instant end);

    // The school's own payments (platform fee excluded), narrowed by whichever of these are given -
    // backs the Payment History and Daily Collection transaction tables. Class is matched through a
    // sub-query on the student's enrollment for the payment's own academic year (a fee structure
    // belongs to one year), scoped to class + section + stream so e.g. "SSS 1 Science" and "SSS 1
    // Art" never get merged into one row.
    default Page<PaymentEntity> searchSchoolPayments(String schoolId, Instant from, Instant to,
            String academicYearId, String termId, String classId, String sectionId, String streamId,
            String studentId, PaymentMethod method, String recordedByUserId, Pageable pageable) {
        Specification<PaymentEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("schoolId"), schoolId),
                cb.isFalse(root.get("fee").get("system")));

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("paidAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("paidAt"), to));
        }
        if (academicYearId != null && !academicYearId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fee").get("academicYear").get("id"),
                    academicYearId));
        }
        if (termId != null && !termId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fee").get("term").get("id"), termId));
        }
        if (method != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), method));
        }
        if (recordedByUserId != null && !recordedByUserId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("recordedByUserId"), recordedByUserId));
        }
        if (studentId != null && !studentId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("student").get("id"), studentId));
        }
        if (classId != null && !classId.isBlank()) {
            String academicYear = academicYearId;
            spec = spec.and((root, query, cb) -> {
                var enrolled = query.subquery(String.class);
                var enrollment = enrolled.from(com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity.class);
                var where = cb.and(
                        cb.equal(enrollment.get("schoolId"), schoolId),
                        cb.equal(enrollment.get("clazz").get("id"), classId));
                if (sectionId != null && !sectionId.isBlank()) {
                    where = cb.and(where, cb.equal(enrollment.get("section").get("id"), sectionId));
                }
                if (streamId != null && !streamId.isBlank()) {
                    where = cb.and(where, cb.equal(enrollment.get("stream").get("id"), streamId));
                }
                if (academicYear != null && !academicYear.isBlank()) {
                    where = cb.and(where, cb.equal(enrollment.get("academicYear").get("id"), academicYear));
                }
                enrolled.select(enrollment.get("student").get("id")).where(where);
                return root.get("student").get("id").in(enrolled);
            });
        }

        return findAll(spec, pageable);
    }
}