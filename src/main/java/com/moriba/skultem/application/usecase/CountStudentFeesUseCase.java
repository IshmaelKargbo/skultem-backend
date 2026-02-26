package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.StudentFeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountStudentFeesUseCase {

    private final StudentFeeRepository repo;

    public Double execute(String school, String studentId) {
        return repo.sumTotalFeeByStudent(studentId, school);
    }
}
