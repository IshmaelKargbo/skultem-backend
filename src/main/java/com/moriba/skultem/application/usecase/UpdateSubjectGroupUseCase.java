package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SubjectGroupMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSubjectGroupUseCase {
    private final SubjectGroupRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUBJECT_GROUP_EDITED")
    public SubjectGroupDTO execute(String schoolId, String id, String name, int totalSelection) {
        var group = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Subject group not found"));

        if (name == null || name.isBlank()) {
            throw new RuleException("Name is required");
        }
        if (totalSelection < 1) {
            throw new RuleException("Total selection must be at least 1");
        }

        var meta = new StringBuilder();
        if (!group.getName().equals(name.trim())) {
            meta.append("name: ").append(group.getName()).append(" → ").append(name.trim());
        }
        if (group.getTotalSelection() != totalSelection) {
            if (meta.length() > 0) meta.append(", ");
            meta.append("totalSelection: ").append(group.getTotalSelection()).append(" → ").append(totalSelection);
        }

        group.update(name.trim(), totalSelection);
        repo.save(group);

        String scopeName = group.getStream() != null ? group.getStream().getName()
                : group.getClazz() != null ? group.getClazz().getName() : "";
        logActivityUseCase.log(schoolId, ActivityType.SUBJECT, "Subject group updated",
                group.getName() + " - " + scopeName, meta.toString(), group.getId());

        return SubjectGroupMapper.toDTO(group);
    }
}
