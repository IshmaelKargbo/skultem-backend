package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.Activity;
import com.moriba.skultem.domain.repository.ActivityRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogActivityUseCase {
    private final ActivityRepository activityRepo;
    private final com.moriba.skultem.application.services.SectionScopeService sectionScopeService;

    @Transactional
    public void log(String schoolId, ActivityType type, String title,
            String subject, String meta, String referenceId) {
        var id = UUID.randomUUID().toString();
        // Done by a section-limited admin -> it belongs to that section (so the other sections' admins don't see it).
        String sectionId = null;
        try {
            var scope = sectionScopeService.currentOrAll();
            if (!scope.wholeSchool() && scope.sectionIds().size() == 1) {
                sectionId = scope.sectionIds().iterator().next();
            }
        } catch (RuntimeException ignored) {
            // No signed-in caller (a scheduled job, an event listener): a school-wide activity.
        }
        var activity = Activity.createInSection(id, schoolId, sectionId, type, title, subject, meta, referenceId);
        activityRepo.save(activity);
    }
}
