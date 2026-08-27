package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PromotionRequestMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReturnPromotionRequestUseCase {

    private final PromotionRequestRepository promotionRequestRepo;

    @AuditLogAnnotation(action = "PROMOTION_RETURNED")
    public PromotionRequestDTO execute(String schoolId, String requestId, String reason) {
        var request = promotionRequestRepo.findByIdAndSchoolId(requestId, schoolId)
                .orElseThrow(() -> new NotFoundException("Promotion request not found"));

        request.returnRequest(reason);
        promotionRequestRepo.save(request);

        return PromotionRequestMapper.toDTO(request);
    }
}
