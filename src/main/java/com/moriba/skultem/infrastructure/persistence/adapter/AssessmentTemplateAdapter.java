package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentTemplateJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AssessmentTemplateMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentTemplateAdapter implements AssessmentTemplateRepository {
    private final AssessmentTemplateJpaRepository repo;

    @Override
    public void save(AssessmentTemplate domain) {
        var entity = AssessmentTemplateMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<AssessmentTemplate> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(AssessmentTemplateMapper::toDomain);
    }

    @Override
    public Page<AssessmentTemplate> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(AssessmentTemplateMapper::toDomain);
    }
}
