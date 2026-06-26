package com.moriba.skultem.application.services;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassFeeDetails;
import com.moriba.skultem.application.usecase.ListStudentFeeByClassUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeService {

    private final ListStudentFeeByClassUseCase classUseCase;

    public ClassFeeDetails getClassFeeDetail(String schoolId, String sessionId, String termId,
            int page, int size) {
        return classUseCase.execute(schoolId, sessionId, termId, page, size);
    }
}
