package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.RequestDemo;
import com.moriba.skultem.domain.repository.RequestDemoRepository;
import com.moriba.skultem.infrastructure.persistence.entity.RequestDemoEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.RequestDemoJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.RequestDemoMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RequestDemoAdapter implements RequestDemoRepository {

    private final RequestDemoJpaRepository repo;

    @Override
    public void save(RequestDemo domain) {
        RequestDemoEntity entity = RequestDemoMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<RequestDemo> findById(String id) {
        return repo.findById(id).map(RequestDemoMapper::toDomain);
    }

    @Override
    public Page<RequestDemo> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(RequestDemoMapper::toDomain);
    }

    @Override
    public void delete(RequestDemo domain) {
        RequestDemoEntity entity = RequestDemoMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public long countAll() {
        return repo.count();
    }
}
