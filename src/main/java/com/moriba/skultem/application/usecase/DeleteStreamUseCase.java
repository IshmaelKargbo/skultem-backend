package com.moriba.skultem.application.usecase;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.StreamUsageRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a stream nothing uses yet - typically one created by mistake. A stream that classes, students, subjects,
 * subject groups, fees or promotions still point at is refused, with what is using it, so nothing is left pointing at
 * a stream that no longer exists. The subjects assigned to the stream itself go with it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteStreamUseCase {
    private final StreamRepository repo;
    private final StreamUsageRepository usageRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STREAM_DELETED")
    public void execute(String schoolId, String id) {
        var stream = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Stream not found"));

        var usage = usageRepo.usage(id);
        if (!usage.isEmpty()) {
            String uses = usage.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", "));
            throw new RuleException(stream.getName() + " is still used by " + uses
                    + ". Move or remove those first, then delete the stream.");
        }

        usageRepo.deleteOwnSubjects(id);
        repo.delete(stream);

        logActivityUseCase.log(schoolId, ActivityType.CLASS, "Stream deleted", stream.getName(), null, id);
    }
}
