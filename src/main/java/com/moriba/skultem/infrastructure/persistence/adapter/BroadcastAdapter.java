package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Broadcast;
import com.moriba.skultem.domain.repository.BroadcastRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.BroadcastJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.BroadcastMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BroadcastAdapter implements BroadcastRepository {
    private final BroadcastJpaRepository repo;

    @Override
    public void save(Broadcast domain) {
        var entity = BroadcastMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Broadcast> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(BroadcastMapper::toDomain);
    }

    @Override
    public Page<Broadcast> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(BroadcastMapper::toDomain);
    }
}
