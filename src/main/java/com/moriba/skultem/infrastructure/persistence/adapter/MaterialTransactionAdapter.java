package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.MaterialTransaction;
import com.moriba.skultem.domain.repository.MaterialTransactionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.MaterialTransactionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.MaterialTransactionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MaterialTransactionAdapter implements MaterialTransactionRepository {
    private final MaterialTransactionJpaRepository repo;

    @Override
    public void save(MaterialTransaction domain) {
        var entity = MaterialTransactionMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<MaterialTransaction> findByMaterialIdAndSchool(String materialId, String schoolId, Pageable pageable) {
        return repo.findAllByMaterialIdAndSchoolIdOrderByCreatedAtDesc(materialId, schoolId, pageable).map(MaterialTransactionMapper::toDomain);
    }
    
}
