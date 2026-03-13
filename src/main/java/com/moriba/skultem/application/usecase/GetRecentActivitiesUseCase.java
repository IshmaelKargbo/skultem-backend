package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ActivityDTO;
import com.moriba.skultem.application.mapper.ActivityMapper;
import com.moriba.skultem.domain.repository.ActivityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetRecentActivitiesUseCase {
    private final ActivityRepository activityRepo;

    public List<ActivityDTO> execute(String schoolId, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        return activityRepo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(0, safeSize))
                .map(ActivityMapper::toDTO)
                .toList();
    }
}
