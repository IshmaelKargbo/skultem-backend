package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SectionMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SectionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Renames a section or changes its description. Everything using it follows, since they point at its id. */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSectionUseCase {
    private final SectionRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SECTION_UPDATED")
    public SectionDTO execute(String schoolId, String id, String name, String description) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new RuleException("Name is required");
        }
        var section = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        if (repo.existsByNameAndSchoolExcept(cleanName, schoolId, id)) {
            throw new AlreadyExistsException("Another section is already called " + cleanName);
        }
        String before = section.getName();
        section.update(cleanName, description == null ? null : description.trim());
        repo.save(section);

        logActivityUseCase.log(schoolId, ActivityType.CLASS, "Section updated",
                before.equals(cleanName) ? cleanName : before + " renamed to " + cleanName, null, id);
        return SectionMapper.toDTO(section);
    }
}
