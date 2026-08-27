package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SumStudentPaymentByFeeThisYearUseCase {

    private final PaymentRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public BigDecimal execute(String schoolId, String studentId, String academicYearId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        return repo.sumPaymentsByStudentThisYear(studentId, academicYear.getId());
    }
}
