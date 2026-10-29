package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.dto.LibraryBookDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.domain.model.LibraryBook;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateLibraryBookUseCase {
    private final LibraryBookRepository repo;
    private final LibraryCategoryRepository categoryRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_BOOK_CREATED")
    public LibraryBookDTO execute(String schoolId, String categoryId, String title, String author, String isbn,
            String publisher, String shelf, int totalCopies) {
        String cleanIsbn = blankToNull(isbn);
        if (cleanIsbn != null && repo.existsByIsbnAndSchool(cleanIsbn, schoolId)) {
            throw new AlreadyExistsException("A book with this ISBN is already in the library");
        }

        var category = categoryRepo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library category not found"));

        var domain = LibraryBook.create(schoolId, category, title.trim(), author.trim(), cleanIsbn,
                blankToNull(publisher), blankToNull(shelf), totalCopies);
        repo.save(domain);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library book added", domain.getTitle(), null,
                domain.getId());

        return LibraryMapper.toDTO(domain);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
