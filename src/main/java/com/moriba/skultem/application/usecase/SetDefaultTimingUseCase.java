package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

// Promotes one Timing template to be the school's default (the fallback used by any Level with no
// explicit assignment), demoting whichever template was default before - there is always exactly
// one, enforced here and by the partial unique index on timings(school_id) where is_default.
@Service
@Transactional
@RequiredArgsConstructor
public class SetDefaultTimingUseCase {
    private final TimingRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TIMING_DEFAULT_SET")
    public Timing execute(String schoolId, String id) {
        var domain = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Timing template not found"));

        if (domain.isDefault()) {
            return domain;
        }

        repo.findDefaultBySchoolId(schoolId).ifPresent(previous -> {
            previous.unmarkAsDefault();
            repo.save(previous);
        });

        domain.markAsDefault();
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Default timing template changed",
                domain.getName(),
                null,
                domain.getId()
        );

        return domain;
    }
}
