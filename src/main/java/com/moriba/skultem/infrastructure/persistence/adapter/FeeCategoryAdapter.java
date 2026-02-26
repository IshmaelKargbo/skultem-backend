package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.FeeCategoryJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.FeeCategoryMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FeeCategoryAdapter implements FeeCategoryRepository {
    private final FeeCategoryJpaRepository repo;

    @Override
    public void save(FeeCategory domain) {
        var entity = FeeCategoryMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<FeeCategory> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(FeeCategoryMapper::toDomain);
    }

    @Override
    public Page<FeeCategory> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(FeeCategoryMapper::toDomain);
    }

    @Override
    public boolean existByNameAndSchoolId(String name, String schoolid) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolid);
    }

    

}
