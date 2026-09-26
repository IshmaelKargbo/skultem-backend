package com.moriba.skultem.domain.repository;

import java.util.Collection;
import java.util.List;

import com.moriba.skultem.domain.model.AssessmentCaEntry;

public interface AssessmentCaEntryRepository {
    void saveAll(List<AssessmentCaEntry> entries);

    List<AssessmentCaEntry> findAllByScoreIds(Collection<String> assessmentScoreIds);
}
