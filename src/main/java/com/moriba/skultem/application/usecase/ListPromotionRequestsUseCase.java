package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PromotionRequestMapper;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListPromotionRequestsUseCase {

    private final PromotionRequestRepository promotionRequestRepo;
    private final TeacherRepository teacherRepo;

    public Page<PromotionRequestDTO> execute(String schoolId, String status, int page, int size) {
        Pageable pageable = createPageable(page, size);

        Page<PromotionRequest> requests = status == null || status.isBlank()
                ? promotionRequestRepo.findBySchoolId(schoolId, pageable)
                : promotionRequestRepo.findBySchoolIdAndStatus(schoolId, PromotionRequest.Status.valueOf(status),
                        pageable);

        return requests.map(PromotionRequestMapper::toDTO);
    }

    public Page<PromotionRequestDTO> executeByUser(String schoolId, String userId, int page, int size) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        Pageable pageable = createPageable(page, size);

        return promotionRequestRepo.findByMasterTeacherIdAndSchoolId(teacher.getId(), schoolId, pageable)
                .map(PromotionRequestMapper::toDTO);
    }

    private Pageable createPageable(int page, int size) {
        if (size <= 0) {
            return Pageable.unpaged();
        }

        return PageRequest.of(Math.max(page - 1, 0), size);
    }
}
