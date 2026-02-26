package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PromoteSchoolUseCase {
    private final ClassRepository classRepo;
    private final PromoteClassUseCase promoteClassUseCase;

    public void execute(String schoolId) {
        var classes = classRepo.findBySchool(schoolId, Pageable.unpaged()).getContent();
        for (var schoolClass : classes) {
            promoteClassUseCase.execute(schoolId, schoolClass.getId());
        }
    }
}
