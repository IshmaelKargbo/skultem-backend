package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.MaterialCategory;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.MaterialCategoryJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.MaterialCategoryMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MaterialCategoryAdapter implements MaterialCategoryRepository {
    private final MaterialCategoryJpaRepository repo;

    @Override
    public void save(MaterialCategory domain) {
        var entity = MaterialCategoryMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existByNameAndSchoolId(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Optional<MaterialCategory> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(MaterialCategoryMapper::toDomain);
    }

    @Override
    public Page<MaterialCategory> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(MaterialCategoryMapper::toDomain);
    }

}
