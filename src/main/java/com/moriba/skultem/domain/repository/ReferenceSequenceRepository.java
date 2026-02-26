package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.ReferenceSequence;

public interface ReferenceSequenceRepository {
    void save(ReferenceSequence param);
    Optional<ReferenceSequence> findForUpdate(String type, Integer year);
}
