package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.moriba.skultem.domain.model.AssessmentTemplate;

public interface AssessmentTemplateRepository {
    void save(AssessmentTemplate domain);

    Optional<AssessmentTemplate> findByIdAndSchoolId(String id, String schoolId);

    Page<AssessmentTemplate> findAllBySchoolId(String schoolId, Pageable pageable);
}
