package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.repository.TransactionRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.jpa.TransactionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TransactionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TransactionAdapter implements TransactionRepository {

    private final TransactionJpaRepository repo;

    @Override
    public void save(Transaction domain) {
        var entity = TransactionMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<Transaction> findAllByAcademicYearAndSchool(String academicYearId, String schoolId, Pageable pageable) {
        return repo.findAllByAcademicYearIdAndSchoolId(academicYearId, schoolId, pageable)
                .map(TransactionMapper::toDomain);
    }

    @Override
    public Page<Transaction> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        return repo.runReport(schoolId, filters, pageable).map(TransactionMapper::toDomain);
    }

    @Override
    public Optional<Transaction> findTopBySchoolId(String schoolId) {
        return repo.findTopBySchoolIdOrderByCreatedAtDesc(schoolId).map(TransactionMapper::toDomain);
    }

}
