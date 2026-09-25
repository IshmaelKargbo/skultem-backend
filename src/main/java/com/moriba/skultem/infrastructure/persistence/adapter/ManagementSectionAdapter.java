package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ManagementSectionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ManagementSectionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ManagementSectionAdapter implements ManagementSectionRepository {
    private final ManagementSectionJpaRepository repo;

    @Override
    public void save(ManagementSection domain) {
        repo.save(ManagementSectionMapper.toEntity(domain));
    }

    @Override
    public List<ManagementSection> findBySchoolId(String schoolId) {
        return repo.findAllBySchoolIdOrderByDisplayOrderAsc(schoolId).stream()
                .map(ManagementSectionMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(ManagementSection domain) {
        repo.deleteById(domain.getId());
    }
}
