package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AssessmentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentAdapter implements AssessmentRepository {
    private final AssessmentJpaRepository repo;

    @Override
    public void save(Assessment domain) {
        repo.save(AssessmentMapper.toEntity(domain));
    }

    @Override
    public void saveAll(List<Assessment> domains) {
        var entities = domains.stream().map(AssessmentMapper::toEntity).toList();
        repo.saveAll(entities);
    }

    @Override
    public void deleteAllByTemplateIdAndSchoolId(String templateId, String schoolId) {
        repo.deleteAllByTemplate_IdAndSchoolId(templateId, schoolId);
    }

    @Override
    public List<Assessment> findAllByTemplateIdAndSchoolId(String templateId, String schoolId) {
        return repo.findAllByTemplate_IdAndSchoolIdOrderByPositionAsc(templateId, schoolId).stream()
                .map(AssessmentMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Assessment> findById(String id) {
        return repo.findById(id).map(AssessmentMapper::toDomain);
    }

    @Override
    public List<Assessment> findAllBySchoolId(String schoolId) {
        return repo.findAllBySchoolIdOrderByPositionAsc(schoolId).stream()
                .map(AssessmentMapper::toDomain)
                .toList();
    }
}
