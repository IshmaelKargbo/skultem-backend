package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
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
public class CreateSubjectUseCase {
    private final SubjectRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUBJECT_CREATED")
    public SubjectDTO execute(String schoolId, String name, String code, String description) {
        if (repo.existsByCodeAndSchool(code, schoolId)) {
            throw new AlreadyExistsException("subject code already exist");
        }
        var id = rg.generate("SUBJECT", "SUB");
        var record = Subject.create(id, schoolId, name, code, description);
        repo.save(record);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SUBJECT,
                "New subject created",
                record.getName() + " (" + record.getCode() + ")",
                null,
                record.getId());

        return SubjectMapper.toDTO(record);
    }
}
