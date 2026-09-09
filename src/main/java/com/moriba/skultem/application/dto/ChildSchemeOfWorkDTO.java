package com.moriba.skultem.application.dto;

import java.util.List;

// One subject's published scheme of work, as a parent sees it: the scheme summary, its rolled-up
// progress/coverage, and the week-by-week topic list. See GetChildCurriculumUseCase.
public record ChildSchemeOfWorkDTO(
        SchemeOfWorkDTO scheme,
        SchemeProgressDTO progress,
        List<WeekDTO> weeks) {
}
