package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.repository.PeriodRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.PeriodJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PeriodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PeriodAdapter implements PeriodRepository {
    private final PeriodJpaRepository repo;

    @Override
    public void save(Period domain) {
        var entity = PeriodMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Period> findById(String id) {
        return repo.findById(id).map(PeriodMapper::toDomain);
    }

    @Override
    public List<Period> findAllBySessionId(String session) {
        return repo.findAllBySessionIdOrderByCreatedAtAsc(session).stream().map(PeriodMapper::toDomain).toList();
    }

    @Override
    public void delete(Period domain) {
        var entity = PeriodMapper.toEntity(domain);
        repo.delete(entity);
    }
}
