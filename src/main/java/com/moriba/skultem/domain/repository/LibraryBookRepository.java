package com.moriba.skultem.domain.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.LibraryBook;

public interface LibraryBookRepository {
    void save(LibraryBook domain);

    void deleteById(String id);

    // In this section (null = books without one).
    boolean existsByIsbnInSection(String isbn, String schoolId, String sectionId);

    boolean existsByCategoryAndSchool(String categoryId, String schoolId);

    Optional<LibraryBook> findByIdAndSchool(String id, String schoolId);

    // Locks the row, so two loans issued at the same moment can't both take the last copy.
    Optional<LibraryBook> findByIdAndSchoolForUpdate(String id, String schoolId);

    // Matches on title, author or ISBN. An empty query or categoryId means "no filter";
    // outOfStockOnly keeps only books with no copy on the shelf. sectionIds: only these sections' books
    // (null = every book). The same goes for the other sectionIds parameters below.
    Page<LibraryBook> search(String schoolId, String query, String categoryId, boolean outOfStockOnly,
            Collection<String> sectionIds, Pageable pageable);

    // Category id -> number of books in it, for the given categories.
    Map<String, Long> countByCategories(String schoolId, Collection<String> categoryIds,
            Collection<String> sectionIds);

    /** Number of titles, total copies and available copies. */
    Totals totals(String schoolId, Collection<String> sectionIds);

    record Totals(long titles, long totalCopies, long availableCopies) {
    }
}
