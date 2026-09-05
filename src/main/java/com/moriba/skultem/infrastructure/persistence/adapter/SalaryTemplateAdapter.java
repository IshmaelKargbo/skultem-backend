package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.SalaryTemplate;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SalaryTemplateJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SalaryTemplateMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SalaryTemplateAdapter implements SalaryTemplateRepository {
    private final SalaryTemplateJpaRepository repo;

    @Override
    public void save(SalaryTemplate domain) {
        repo.save(SalaryTemplateMapper.toEntity(domain));
    }

    @Override
    public Optional<SalaryTemplate> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(SalaryTemplateMapper::toDomain);
    }

    @Override
    public Page<SalaryTemplate> search(String schoolId, String query, Pageable pageable) {
        return repo.search(schoolId, query, pageable).map(SalaryTemplateMapper::toDomain);
    }

    @Override
    public Page<SalaryTemplate> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(SalaryTemplateMapper::toDomain);
    }

    @Override
    public void deleteByIdAndSchoolId(String id, String schoolId) {
        repo.deleteByIdAndSchoolId(id, schoolId);
    }
}
