package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlaygroundSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.PlaygroundDataCategory;
import com.moriba.skultem.domain.repository.PlaygroundDataRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Shows a school what it has created while in playground mode, so it can pick what to clear
 * before going live (see WipeTestSchoolDataUseCase). Counts are only gathered while the school is
 * actually a playground - a live school gets its flag back with no categories.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetPlaygroundSummaryUseCase {
    private final SchoolRepository schoolRepo;
    private final PlaygroundDataRepository playgroundDataRepo;

    public PlaygroundSummaryDTO execute(String schoolId) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));

        List<PlaygroundSummaryDTO.Category> categories = List.of();
        if (school.isTestSchool()) {
            Map<PlaygroundDataCategory, Map<String, Long>> counts = playgroundDataRepo.countByCategory(schoolId);
            categories = counts.entrySet().stream()
                    .map(e -> new PlaygroundSummaryDTO.Category(e.getKey(), e.getValue(), e.getKey().requires()))
                    .toList();
        }

        return new PlaygroundSummaryDTO(school.getId(), school.getName(), school.getDomain(), school.isTestSchool(),
                categories);
    }
}
