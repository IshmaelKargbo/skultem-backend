package com.moriba.skultem.infrastructure.persistence.adapter;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.BehaviourCategory;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.BehaviourCategoryJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.BehaviourCategoryMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BehaviourCategoryAdapter implements BehaviourCategoryRepository {

    private final BehaviourCategoryJpaRepository repo;

    @Override
    public void save(BehaviourCategory domain) {
        var entity = BehaviourCategoryMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<BehaviourCategory> findAllSchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtAsc(schoolId, pageable).map(BehaviourCategoryMapper::toDomain);
    }

    
}
