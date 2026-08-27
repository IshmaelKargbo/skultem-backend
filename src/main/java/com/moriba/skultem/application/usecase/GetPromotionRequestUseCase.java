package com.moriba.skultem.application.usecase;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PromotionRequestMapper;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetPromotionRequestUseCase {

    private final PromotionRequestRepository promotionRequestRepo;
    private final ClassSessionRepository classSessionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ComputeEnrollmentYearAverageUseCase computeEnrollmentYearAverageUseCase;

    public PromotionRequestDTO execute(String schoolId, String requestId) {
        var request = promotionRequestRepo.findByIdAndSchoolId(requestId, schoolId)
                .orElseThrow(() -> new NotFoundException("Promotion request not found"));

        return PromotionRequestMapper.toDTO(request, averagesFor(schoolId, request));
    }

    /**
     * The current open (or most recently approved) promotion request for a class session, if any -
     * used by the class master's review screen to resume a returned submission or show a completed one.
     */
    public PromotionRequestDTO executeBySession(String schoolId, String sessionId) {
        classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var activeAcademicYear = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
        if (activeAcademicYear == null) {
            return null;
        }

        return promotionRequestRepo
                .findOpenBySessionIdAndAcademicYearIdAndSchoolId(sessionId, activeAcademicYear.getId(), schoolId)
                .map(request -> PromotionRequestMapper.toDTO(request, averagesFor(schoolId, request)))
                .orElse(null);
    }

    private Map<String, Double> averagesFor(String schoolId, PromotionRequest request) {
        Map<String, Double> averages = new HashMap<>();
        for (var item : request.getItems()) {
            if (item.getEnrollment() == null) {
                continue;
            }
            averages.put(item.getEnrollment().getId(),
                    computeEnrollmentYearAverageUseCase.execute(schoolId, item.getEnrollment().getId()));
        }
        return averages;
    }
}
