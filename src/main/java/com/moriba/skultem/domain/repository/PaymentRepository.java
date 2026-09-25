package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.vo.Level;

import java.util.Collection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.FeeCategoryRevenue;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.vo.Filter;

public interface PaymentRepository {

    void save(Payment domain);

    Page<Payment> findByStudent(String studentId, Pageable pageable);

    /** A student's payments towards the fees of one academic year only. */
    Page<Payment> findByStudentAndAcademicYear(String studentId, String academicYearId, Pageable pageable);

    List<Payment> findAllByReferenceNoAndSchoolId(String referenceNo, String schoolId);

    Page<Payment> findAllByAcademicYearAndSchoolId(String academicYearId, String schoolId, Pageable pageable);

    // Only payments by students enrolled that year at these levels (see SectionScope).
    Page<Payment> findAllByAcademicYearAndSchoolId(String academicYearId, String schoolId, Collection<Level> levels,
            Pageable pageable);

    BigDecimal sumPaymentsByStudentAndFee(String studentId, String feeId);

    BigDecimal sumPaymentsByFeeAndSchool(String feeId, String schoolId);


    BigDecimal sumPaymentsByStudentThisYear(String studentId, String academicYearId);

    BigDecimal sumPaymentsBySchool(String schoolId);

    BigDecimal sumPaymentsBySchoolAndDateRange(String schoolId, Instant start, Instant end);

    BigDecimal sumPaymentsBySchoolAndDateRange(String schoolId, Instant start, Instant end,
            Collection<Level> levels);

    List<FeeCategoryRevenue> sumRevenueByCategory(String schoolId);

    List<FeeCategoryRevenue> sumRevenueByCategory(String schoolId, Collection<Level> levels);

    // levels: always applied (full catalog for whole-school callers) - see SectionScope.
    Page<Payment> runReport(String schoolId, List<Filter> filters, Collection<Level> levels,
            Pageable pageable);

    /** Same as sumPaymentsBySchool, but the platform fee is excluded. */
    BigDecimal sumSchoolPaymentsBySchool(String schoolId);

    /** Paid-so-far per (student, fee) pair for the school's own fees - row shape [studentId, feeId, totalPaid]. */
    List<Object[]> sumSchoolPaymentsGroupedByStudentAndFee(String schoolId, String academicYearId, String termId);

    /** School-fee payments in [start, end), grouped by method - row shape [method, totalAmount, count]. */
    List<Object[]> sumSchoolPaymentsByMethodAndDateRange(String schoolId, Instant start, Instant end);

    /** The school's own payments (platform fee excluded), narrowed by whichever of these are given. */
    Page<Payment> searchSchoolPayments(String schoolId, Instant from, Instant to, String academicYearId,
            String termId, String classId, String sectionId, String streamId, String studentId,
            PaymentMethod method, String recordedByUserId, Pageable pageable);
}