package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentCaEntryEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentCaEntryJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentCaEntryAdapter implements AssessmentCaEntryRepository {

    private final AssessmentCaEntryJpaRepository repo;

    @Override
    public void saveAll(List<AssessmentCaEntry> entries) {
        repo.saveAll(entries.stream().map(e -> AssessmentCaEntryEntity.builder()
                .id(e.getId())
                .schoolId(e.getSchoolId())
                .assessmentScoreId(e.getAssessmentScoreId())
                .entryNumber(e.getEntryNumber())
                .score(e.getScore())
                .recordedByUserId(e.getRecordedByUserId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build()).toList());
    }

    @Override
    public List<AssessmentCaEntry> findAllByScoreIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repo.findAllByAssessmentScoreIdIn(ids).stream()
                .map(e -> new AssessmentCaEntry(e.getId(), e.getSchoolId(), e.getAssessmentScoreId(),
                        e.getEntryNumber(), e.getScore(), e.getRecordedByUserId(), e.getCreatedAt(),
                        e.getUpdatedAt()))
                .toList();
    }
}
