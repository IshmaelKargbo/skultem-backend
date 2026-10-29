package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;

/** Blocked while any book is still in the category - those books have to be moved or removed first. */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteLibraryCategoryUseCase {
    private final LibraryCategoryRepository repo;
    private final LibraryBookRepository bookRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_CATEGORY_DELETED")
    public void execute(String schoolId, String id) {
        var category = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Library category not found"));

        if (bookRepo.existsByCategoryAndSchool(id, schoolId)) {
            throw new RuleException(
                    "This category still has books in it. Move those books to another category or remove them first.");
        }

        repo.deleteById(id);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library category deleted", category.getName(),
                null, id);
    }
}
