package com.moriba.skultem.infrastructure.persistence.adapter;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.repository.PaymentRepository;
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
}
