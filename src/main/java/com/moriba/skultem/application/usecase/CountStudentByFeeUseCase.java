package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.StudentFeeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CountStudentByFeeUseCase {

    private final StudentFeeRepository repo;

    public long execute(String schoolId, String feeId) {
        return repo.countByFeeAndSchool(feeId, schoolId);
    }
}
