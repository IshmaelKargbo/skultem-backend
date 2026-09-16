package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSubjectUseCase {
    private final SubjectRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUBJECT_EDITED")
    public SubjectDTO execute(String schoolId, String subjectId, String name, String code, String description) {
        Subject subject = repo.findByIdAndSchoolId(subjectId, schoolId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));

        if (repo.existsByCodeAndSchoolAndIdNot(code, schoolId, subjectId)) {
            throw new AlreadyExistsException("Subject code already exists");
        }

        String oldName = subject.getName();
        String oldCode = subject.getCode();
        String oldDescription = subject.getDescription();

        subject.update(name, code, description);
        repo.save(subject);

        String meta = buildMeta(oldName, name, oldCode, code, oldDescription, description);

        logActivityUseCase.log(schoolId, ActivityType.SUBJECT, "Subject updated",
                subject.getName() + " (" + subject.getCode() + ")", meta, subject.getId());

        return SubjectMapper.toDTO(subject);
    }

    private String buildMeta(String oldName, String newName, String oldCode, String newCode,
            String oldDescription, String newDescription) {
        StringBuilder meta = new StringBuilder();

        append(meta, "name", oldName, newName);
        append(meta, "code", oldCode, newCode);
        append(meta, "description", oldDescription, newDescription);

        return meta.toString();
    }

    private void append(StringBuilder meta, String field, String oldVal, String newVal) {
        if (oldVal == null && newVal == null)
            return;
        if (oldVal != null && oldVal.equals(newVal))
            return;

        if (meta.length() > 0)
            meta.append(", ");

        meta.append(field).append(": ").append(oldVal).append(" → ").append(newVal);
    }
}
