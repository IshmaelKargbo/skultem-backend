package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.TimingLevel;
import com.moriba.skultem.domain.repository.TimingLevelRepository;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Assigns a Timing template to a Level (PRIMARY/JSS/SSS). A level maps to at most one template at
// a time - reassigning just overwrites the existing mapping, it doesn't create a second one.
// timingId may point to the same template already used by another level - that's the whole point
// (e.g. one shared "Secondary" template covering both JSS and SSS).
@Service
@Transactional
@RequiredArgsConstructor
public class AssignTimingLevelUseCase {
    private final TimingRepository timingRepo;
    private final TimingLevelRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TIMING_LEVEL_ASSIGNED")
    public TimingLevel execute(String schoolId, Level level, String timingId) {
        var timing = timingRepo.findByIdAndSchoolId(timingId, schoolId)
                .orElseThrow(() -> new NotFoundException("Timing template not found"));

        var domain = repo.findBySchoolIdAndLevel(schoolId, level)
                .map(existing -> {
                    existing.reassign(timing);
                    return existing;
                })
                .orElseGet(() -> TimingLevel.create(UUID.randomUUID().toString(), schoolId, timing, level));

        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Timing template assigned",
                level.name() + " -> " + timing.getName(),
                null,
                domain.getId()
        );

        return domain;
    }
}
