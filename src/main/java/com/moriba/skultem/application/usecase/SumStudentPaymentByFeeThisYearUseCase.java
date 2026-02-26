package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SumStudentPaymentByFeeThisYearUseCase {

    private final PaymentRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public BigDecimal execute(String schoolId, String studentId) {
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("academic year not found"));
        return repo.sumPaymentsByStudentThisYear(studentId, academicYear.getId());
    }
}
