package com.moriba.skultem.application.dto;

import java.util.List;
import java.util.Map;

import com.moriba.skultem.domain.model.PlaygroundDataCategory;

/**
 * What a playground school has accumulated while testing, per category it can clear before going
 * live - see GetPlaygroundSummaryUseCase. {@code requires} mirrors
 * {@link PlaygroundDataCategory#requires()} so the UI can tick dependents along with a selection.
 */
public record PlaygroundSummaryDTO(String schoolId, String schoolName, String domain, boolean testSchool,
        List<Category> categories) {

    public record Category(PlaygroundDataCategory key, Map<String, Long> counts,
            List<PlaygroundDataCategory> requires) {
    }
}
