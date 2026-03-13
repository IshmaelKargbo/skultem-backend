package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.SectionMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.SectionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateSectionUseCase {

    private final SectionRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SECTION_CREATED")
    public SectionDTO execute(String school, String name, String description) {
        if (repo.existfindByNameAndSchoolId(name, school)) {
            throw new AlreadyExistsException("section already exist");
        }

        var id = rg.generate("SECTION", "SEC");
        var record = Section.create(id, school, name, description);
        repo.save(record);

        logActivityUseCase.log(
                school,
                ActivityType.CLASS,
                "New section created",
                record.getName(),
                null,
                record.getId());

        return SectionMapper.toDTO(record);
    }
}
