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

    @Transactional
    public void log(String schoolId, ActivityType type, String title,
            String subject, String meta, String referenceId) {
        var id = UUID.randomUUID().toString();
        var activity = Activity.create(id, schoolId, type,
                title, subject, meta, referenceId);
        activityRepo.save(activity);
    }
}
