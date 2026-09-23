package com.moriba.skultem.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.FeeCategoryRevenue;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.jpa.PaymentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PaymentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentRepository {
    private final PaymentJpaRepository repo;

    @Override
    public void save(Payment domain) {
        var entity = PaymentMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<Payment> findAllByAcademicYearAndSchoolId(String academicYearId, String schoolId, Pageable pageable) {
        return repo.findAllByFee_AcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(academicYearId, schoolId, pageable).map(PaymentMapper::toDomain);
    }

    @Override
    public Page<Payment> findByStudent(String studentId, Pageable pageable) {
        return repo.findAllByStudent_Id(studentId, pageable).map(PaymentMapper::toDomain);
    }

    @Override
    public Page<Payment> findByStudentAndAcademicYear(String studentId, String academicYearId, Pageable pageable) {
        return repo.findAllByStudent_IdAndFee_AcademicYear_Id(studentId, academicYearId, pageable)
                .map(PaymentMapper::toDomain);
    }

    @Override
    public List<Payment> findAllByReferenceNoAndSchoolId(String referenceNo, String schoolId) {
        return repo.findAllByReferenceNoAndSchoolIdOrderByCreatedAtAsc(referenceNo, schoolId).stream()
                .map(PaymentMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumPaymentsByStudentAndFee(String studentId, String feeId) {
        return repo.sumPaymentsByStudentAndFee(studentId, feeId);
    }

    @Override
    public BigDecimal sumPaymentsByFeeAndSchool(String feeId, String schoolId) {
        return repo.sumPaymentsByFeeAndSchool(feeId, schoolId);
    }

    @Override
    public BigDecimal sumPaymentsBySchool(String schoolId) {
        return repo.sumPaymentsBySchool(schoolId);
    }

    @Override
    public BigDecimal sumPaymentsByStudentThisYear(String studentId, String academicYearId) {
        return repo.sumPaymentsByStudentThisYear(studentId, academicYearId);
    }

    @Override
    public BigDecimal sumPaymentsBySchoolAndDateRange(String schoolId, Instant start, Instant end) {
        return repo.sumPaymentsBySchoolAndDateRange(schoolId, start, end);
    }

    @Override
    public List<FeeCategoryRevenue> sumRevenueByCategory(String schoolId) {
        return repo.sumRevenueByCategory(schoolId);
    }

    @Override
    public Page<Payment> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        return repo.runReport(schoolId, filters, pageable)
                .map(PaymentMapper::toDomain);
    }

    @Override
    public BigDecimal sumSchoolPaymentsBySchool(String schoolId) {
        return repo.sumSchoolPaymentsBySchool(schoolId);
    }

    @Override
    public List<Object[]> sumSchoolPaymentsGroupedByStudentAndFee(String schoolId, String academicYearId,
            String termId) {
        return repo.sumSchoolPaymentsGroupedByStudentAndFee(schoolId, academicYearId, termId == null ? "" : termId);
    }

    @Override
    public List<Object[]> sumSchoolPaymentsByMethodAndDateRange(String schoolId, Instant start, Instant end) {
        return repo.sumSchoolPaymentsByMethodAndDateRange(schoolId, start, end);
    }

    @Override
    public Page<Payment> searchSchoolPayments(String schoolId, Instant from, Instant to, String academicYearId,
            String termId, String classId, String sectionId, String streamId, String studentId,
            Payment.PaymentMethod method, String recordedByUserId, Pageable pageable) {
        return repo.searchSchoolPayments(schoolId, from, to, academicYearId, termId, classId, sectionId, streamId,
                studentId, method, recordedByUserId, pageable).map(PaymentMapper::toDomain);
    }
}
