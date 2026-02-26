package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ReferenceSequence;
import com.moriba.skultem.domain.repository.ReferenceSequenceRepository;
import com.moriba.skultem.infrastructure.persistence.entity.ReferenceSequenceEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ReferenceSequenceJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ReferenceSequenceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReferenceSequenceAdapter implements ReferenceSequenceRepository {

    private final ReferenceSequenceJpaRepository repo;

    @Override
    public Optional<ReferenceSequence> findForUpdate(String type, Integer year) {
        Optional<ReferenceSequenceEntity> optional = repo.findForUpdate(type, year);

        return optional.isPresent()
                ? optional.map(ReferenceSequenceMapper::toDomain)
                : Optional.of(ReferenceSequenceMapper.toDomain(createNewSequence(type, year)));
    }

    private ReferenceSequenceEntity createNewSequence(String type, Integer year) {
        return ReferenceSequenceEntity.builder()
                .year(year)
                .referenceType(type)
                .lastNumber(0)
                .build();
    }

    @Override
    public void save(ReferenceSequence param) {
        var entity = ReferenceSequenceMapper.toEntity(param);
        repo.save(entity);
    }
}
