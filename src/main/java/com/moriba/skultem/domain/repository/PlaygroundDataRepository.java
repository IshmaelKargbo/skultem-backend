package com.moriba.skultem.domain.repository;

import java.util.Map;
import java.util.Set;

import com.moriba.skultem.domain.model.PlaygroundDataCategory;

/**
 * Cross-table access to a playground school's own activity data, grouped by
 * {@link PlaygroundDataCategory} - see WipeTestSchoolDataUseCase. Kept as one port rather than a
 * delete/count method on every individual repository, since clearing a category safely depends on
 * the foreign keys between those tables and that ordering belongs in one place.
 */
public interface PlaygroundDataRepository {

    /** Per category, a label -> row count breakdown of what's there (e.g. "Students" -> 42). */
    Map<PlaygroundDataCategory, Map<String, Long>> countByCategory(String schoolId);

    /**
     * Removes (or, where the rows must survive, resets) every category in {@code categories} for
     * this school. Expects the selection to already be closed over
     * {@link PlaygroundDataCategory#withDependencies} and the caller to hold a transaction.
     */
    void wipe(String schoolId, Set<PlaygroundDataCategory> categories);
}
