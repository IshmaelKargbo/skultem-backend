package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.GradeApprovalRoutingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.GradeApprovalResolver;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetGradeApprovalRoutingUseCase {

    private final SchoolRepository schoolRepo;
    private final GradeApprovalResolver resolver;

    public GradeApprovalRoutingDTO execute(String schoolId) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        return resolver.routing(school);
    }
}
