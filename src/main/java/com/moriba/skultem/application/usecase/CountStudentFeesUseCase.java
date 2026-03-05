package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.StudentFeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountStudentFeesUseCase {

    private final StudentFeeRepository repo;

    public BigDecimal execute(String school, String studentId) {
        return repo.sumTotalFeeByStudent(studentId, school);
    }
}
