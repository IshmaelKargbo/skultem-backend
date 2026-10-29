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

    boolean existsByIsbnAndSchool(String isbn, String schoolId);

    boolean existsByCategoryAndSchool(String categoryId, String schoolId);

    Optional<LibraryBook> findByIdAndSchool(String id, String schoolId);

    // Locks the row, so two loans issued at the same moment can't both take the last copy.
    Optional<LibraryBook> findByIdAndSchoolForUpdate(String id, String schoolId);

    // Matches on title, author or ISBN. An empty query or categoryId means "no filter";
    // outOfStockOnly keeps only books with no copy on the shelf.
    Page<LibraryBook> search(String schoolId, String query, String categoryId, boolean outOfStockOnly,
            Pageable pageable);

    // Category id -> number of books in it, for the given categories.
    Map<String, Long> countByCategories(String schoolId, Collection<String> categoryIds);

    /** Number of titles, total copies and available copies across the whole school. */
    Totals totals(String schoolId);

    record Totals(long titles, long totalCopies, long availableCopies) {
    }
}
