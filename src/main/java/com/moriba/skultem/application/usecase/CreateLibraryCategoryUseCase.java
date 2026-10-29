package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.dto.LibraryCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.domain.model.LibraryCategory;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateLibraryCategoryUseCase {
    private final LibraryCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_CATEGORY_CREATED")
    public LibraryCategoryDTO execute(String schoolId, String name, String description) {
        String trimmed = name.trim();
        if (repo.existsByNameAndSchool(trimmed, schoolId)) {
            throw new AlreadyExistsException("A library category with this name already exists");
        }

        var domain = LibraryCategory.create(schoolId, trimmed, description);
        repo.save(domain);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library category created", domain.getName(), null,
                domain.getId());

        return LibraryMapper.toDTO(domain);
    }
}
