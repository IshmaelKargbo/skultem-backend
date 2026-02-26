package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SumStudentPaymentByFeeUseCase {

    private final PaymentRepository repo;

    public BigDecimal execute(String fee, String studentId) {
        return repo.sumPaymentsByStudentAndFee(studentId, fee);
    }
}
