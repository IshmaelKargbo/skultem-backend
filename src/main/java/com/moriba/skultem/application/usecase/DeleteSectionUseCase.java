package com.moriba.skultem.application.usecase;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SectionRepository;
import com.moriba.skultem.domain.repository.SectionUsageRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a section nothing uses yet - typically one created by mistake. A section that classes, class sessions or
 * student enrollments still point at is refused, with what is using it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteSectionUseCase {
    private final SectionRepository repo;
    private final SectionUsageRepository usageRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SECTION_DELETED")
    public void execute(String schoolId, String id) {
        var section = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Section not found"));

        var usage = usageRepo.usage(id);
        if (!usage.isEmpty()) {
            String uses = usage.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", "));
            throw new RuleException(section.getName() + " is still used by " + uses
                    + ". Move or remove those first, then delete the section.");
        }

        repo.delete(section);
        logActivityUseCase.log(schoolId, ActivityType.CLASS, "Section deleted", section.getName(), null, id);
    }
}
