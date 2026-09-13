package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.TimingLevelRepository;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.repository.WorkingDayRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

// Deletes a Timing template. The default template can't be deleted - every school must always
// have exactly one fallback for levels without their own assignment, so promote another template
// first (SetDefaultTimingUseCase) if the current default needs to go. Any Level currently pointing
// at the deleted template is unassigned rather than left dangling - it just falls back to whatever
// is default from then on.
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteTimingUseCase {
    private final TimingRepository repo;
    private final TimingLevelRepository timingLevelRepo;
    private final WorkingDayRepository workingDayRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TIMING_DELETED")
    public Timing execute(String schoolId, String id) {
        var domain = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Timing template not found"));

        if (domain.isDefault()) {
            throw new BadRequestException(
                    "Can't delete the default timing template - set another template as default first");
        }

        timingLevelRepo.deleteByTimingId(id);
        workingDayRepo.deleteAllByTimingId(id);
        repo.delete(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Timing template deleted",
                domain.getName(),
                null,
                domain.getId()
        );

        return domain;
    }
}
