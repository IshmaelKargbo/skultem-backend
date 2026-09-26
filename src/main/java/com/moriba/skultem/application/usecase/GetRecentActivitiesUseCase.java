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
    private final com.moriba.skultem.application.services.SectionScopeService sectionScopeService;

    public List<ActivityDTO> execute(String schoolId, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        var scope = sectionScopeService.effective();
        var page = scope.wholeSchool()
                ? activityRepo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(0, safeSize))
                : activityRepo.findVisibleToSections(schoolId, scope.sectionIds(), PageRequest.of(0, safeSize));
        return page.map(ActivityMapper::toDTO).toList();
    }
}
