package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.SectionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SectionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SectionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SectionAdapter implements SectionRepository {
    private final SectionJpaRepository repo;

    @Override
    public void save(Section domain) {
        var entity = SectionMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Section> findById(String id) {
        return repo.findById(id).map(SectionMapper::toDomain);
    }

    @Override
    public void delete(Section domain) {
        var entity = SectionMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public Optional<Section> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(SectionMapper::toDomain);
    }

    @Override
    public Page<Section> findBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(SectionMapper::toDomain);
    }

    @Override
    public Optional<Section> findByNameAndSchoolId(String name, String schoolId) {
        return repo.findByNameAndSchoolId(name, schoolId).map(SectionMapper::toDomain);
    }

    @Override
    public boolean existfindByNameAndSchoolId(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }
}
