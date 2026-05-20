package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.MaterialJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.MaterialMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MaterialAdapter implements MaterialRepository {
    private final MaterialJpaRepository repo;

    @Override
    public void save(Material domain) {
        var entity = MaterialMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existByNameAndSchoolId(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Optional<Material> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(MaterialMapper::toDomain);
    }

    @Override
    public Page<Material> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(MaterialMapper::toDomain);
    }

}
