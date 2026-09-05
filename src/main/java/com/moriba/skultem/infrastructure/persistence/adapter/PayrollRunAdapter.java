package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PayrollRun;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.PayrollRunJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PayrollRunMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PayrollRunAdapter implements PayrollRunRepository {
    private final PayrollRunJpaRepository repo;

    @Override
    public void save(PayrollRun domain) {
        repo.save(PayrollRunMapper.toEntity(domain));
    }

    @Override
    public Optional<PayrollRun> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(PayrollRunMapper::toDomain);
    }

    @Override
    public Page<PayrollRun> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(PayrollRunMapper::toDomain);
    }

    @Override
    public Page<PayrollRun> search(String schoolId, String query, PayrollRun.Status status, Pageable pageable) {
        return repo.search(schoolId, query, status, pageable).map(PayrollRunMapper::toDomain);
    }

    @Override
    public Optional<PayrollRun> findLatestBySchoolId(String schoolId) {
        return repo.findFirstBySchoolIdOrderByCreatedAtDesc(schoolId).map(PayrollRunMapper::toDomain);
    }
}
