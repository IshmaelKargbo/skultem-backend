package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.StreamMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Renames a stream or changes its description. Everything using it follows, since they point at its id. */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateStreamUseCase {
    private final StreamRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STREAM_UPDATED")
    public StreamDTO execute(String schoolId, String id, String name, String description) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new RuleException("Name is required");
        }
        var stream = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Stream not found"));
        if (repo.existsByNameAndSchoolExcept(cleanName, schoolId, id)) {
            throw new AlreadyExistsException("Another stream is already called " + cleanName);
        }
        String before = stream.getName();
        stream.update(cleanName, description == null ? null : description.trim());
        repo.save(stream);

        logActivityUseCase.log(schoolId, ActivityType.CLASS, "Stream updated",
                before.equals(cleanName) ? cleanName : before + " renamed to " + cleanName, null, id);
        return StreamMapper.toDTO(stream);
    }
}
