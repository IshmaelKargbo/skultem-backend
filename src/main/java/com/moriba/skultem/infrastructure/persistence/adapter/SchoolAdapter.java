package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.SchoolJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SchoolMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolAdapter implements SchoolRepository {

    private final SchoolJpaRepository repo;

    @Override
    public void save(School domain) {
        SchoolEntity entity = SchoolMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<School> findById(String id) {
        return repo.findById(id).map(SchoolMapper::toDomain);
    }

    @Override
    public boolean existsByDomain(String domain) {
        return repo.existsByDomainIgnoreCase(domain);
    }

    @Override
    public Page<School> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(SchoolMapper::toDomain);
    }

    @Override
    public void delete(School domain) {
        SchoolEntity entity = SchoolMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public Optional<School> findByDomain(String domain) {
        return repo.findByDomainIgnoreCase(domain).map(SchoolMapper::toDomain);
    }
}
