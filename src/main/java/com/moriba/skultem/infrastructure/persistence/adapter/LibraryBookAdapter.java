package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibraryBook;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibraryBookJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryBookMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibraryBookAdapter implements LibraryBookRepository {
    private final LibraryBookJpaRepository repo;

    @Override
    public void save(LibraryBook domain) {
        repo.save(LibraryBookMapper.toEntity(domain));
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByIsbnInSection(String isbn, String schoolId, String sectionId) {
        return repo.existsByIsbnIgnoreCaseAndSchoolIdAndManagementSectionId(isbn, schoolId, sectionId);
    }

    @Override
    public boolean existsByCategoryAndSchool(String categoryId, String schoolId) {
        return repo.existsByCategory_IdAndSchoolId(categoryId, schoolId);
    }

    @Override
    public Optional<LibraryBook> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(LibraryBookMapper::toDomain);
    }

    @Override
    public Optional<LibraryBook> findByIdAndSchoolForUpdate(String id, String schoolId) {
        return repo.findForUpdate(id, schoolId).map(LibraryBookMapper::toDomain);
    }

    @Override
    public Page<LibraryBook> search(String schoolId, String query, String categoryId, boolean outOfStockOnly,
            Collection<String> sectionIds, Pageable pageable) {
        return repo.search(schoolId, query == null ? "" : query, categoryId == null ? "" : categoryId,
                outOfStockOnly, all(sectionIds), ids(sectionIds), pageable)
                .map(LibraryBookMapper::toDomain);
    }

    @Override
    public Map<String, Long> countByCategories(String schoolId, Collection<String> categoryIds,
            Collection<String> sectionIds) {
        Map<String, Long> counts = new HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) return counts;

        for (Object[] row : repo.countByCategories(schoolId, categoryIds, all(sectionIds), ids(sectionIds))) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public Totals totals(String schoolId, Collection<String> sectionIds) {
        var rows = repo.totals(schoolId, all(sectionIds), ids(sectionIds));
        if (rows.isEmpty()) return new Totals(0, 0, 0);

        Object[] row = rows.get(0);
        return new Totals(((Number) row[0]).longValue(), ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue());
    }

    // Queries take "every section" as a flag plus a list that's never empty (an empty IN list isn't valid SQL).
    static boolean all(Collection<String> sectionIds) {
        return sectionIds == null;
    }

    static Collection<String> ids(Collection<String> sectionIds) {
        return sectionIds == null || sectionIds.isEmpty() ? List.of("") : sectionIds;
    }
}
