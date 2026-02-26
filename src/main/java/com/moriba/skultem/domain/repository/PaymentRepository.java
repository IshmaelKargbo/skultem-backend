package com.moriba.skultem.domain.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Payment;

public interface PaymentRepository {
    void save(Payment domain);

    Page<Payment> findByStudent(String studentId, Pageable pageable);

    Page<Payment> findAllByAcademicYearAndSchoolId(String academicYearId, String schoolId, Pageable pageable);

    BigDecimal sumPaymentsByStudentAndFee(String studentId, String feeId);

    BigDecimal sumPaymentsByFeeAndSchool(String feeId, String schoolId);

    BigDecimal sumPaymentsByStudentThisYear(String studentId, String academicYearId);

    BigDecimal sumPaymentsBySchool(String schoolId);
}
