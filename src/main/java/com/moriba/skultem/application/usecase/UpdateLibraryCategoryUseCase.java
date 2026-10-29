package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.dto.LibraryCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateLibraryCategoryUseCase {
    private final LibraryCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_CATEGORY_UPDATED")
    public LibraryCategoryDTO execute(String schoolId, String id, String name, String description) {
        var category = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Library category not found"));

        String trimmed = name.trim();
        if (!category.getName().equalsIgnoreCase(trimmed) && repo.existsByNameAndSchool(trimmed, schoolId)) {
            throw new AlreadyExistsException("A library category with this name already exists");
        }

        category.update(trimmed, description);
        repo.save(category);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library category updated", category.getName(),
                null, category.getId());

        return LibraryMapper.toDTO(category);
    }
}
