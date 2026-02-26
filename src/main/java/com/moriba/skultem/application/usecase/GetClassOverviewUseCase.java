package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassOverviewDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetClassOverviewUseCase {
    private final GetClassUseCase getClassUseCase;
    private final ListClassSectionByClassUseCase listClassSectionByClassUseCase;
    private final ListClassStreamByIdUseCase listClassStreamByIdUseCase;
    private final GetCurrentClassMasterUseCase getCurrentClassMasterUseCase;

    public ClassOverviewDTO execute(String schoolId, String classId) {
        var clazz = getClassUseCase.execute(schoolId, classId);
        var sections = listClassSectionByClassUseCase.execute(schoolId, classId);
        var streams = listClassStreamByIdUseCase.execute(schoolId, classId);
        var masters = getCurrentClassMasterUseCase.execute(schoolId, classId);

        return new ClassOverviewDTO(
                clazz,
                sections.size(),
                streams.size(),
                masters.size(),
                sections,
                streams,
                masters);
    }
}
