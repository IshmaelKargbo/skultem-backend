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
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateLibraryBookUseCase {
    private final LibraryBookRepository repo;
    private final LibraryCategoryRepository categoryRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_BOOK_UPDATED")
    public LibraryBookDTO execute(String schoolId, String id, String categoryId, String title, String author,
            String isbn, String publisher, String shelf, int totalCopies) {
        // Locked so a loan issued or returned at the same time can't throw the copy count off.
        var book = repo.findByIdAndSchoolForUpdate(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        String cleanIsbn = CreateLibraryBookUseCase.blankToNull(isbn);
        boolean isbnChanged = cleanIsbn != null && !cleanIsbn.equalsIgnoreCase(book.getIsbn());
        if (isbnChanged && repo.existsByIsbnAndSchool(cleanIsbn, schoolId)) {
            throw new AlreadyExistsException("A book with this ISBN is already in the library");
        }

        var category = categoryRepo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library category not found"));

        book.update(category, title.trim(), author.trim(), cleanIsbn,
                CreateLibraryBookUseCase.blankToNull(publisher), CreateLibraryBookUseCase.blankToNull(shelf),
                totalCopies);
        repo.save(book);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library book updated", book.getTitle(), null,
                book.getId());

        return LibraryMapper.toDTO(book);
    }
}
