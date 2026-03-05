package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.Assessment;

public interface AssessmentRepository {
    void save(Assessment domain);

    void saveAll(List<Assessment> domains);

    void deleteAllByTemplateIdAndSchoolId(String templateId, String schoolId);

    List<Assessment> findAllByTemplateIdAndSchoolId(String templateId, String schoolId);

    Optional<Assessment> findById(String id);
}
