package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Expense;
import com.moriba.skultem.domain.repository.ExpenseRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ExpenseJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ExpenseMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExpenseAdapter implements ExpenseRepository {
    private final ExpenseJpaRepository repo;

    @Override
    public void save(Expense domain) {
        var entity = ExpenseMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Expense> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ExpenseMapper::toDomain);
    }

    @Override
    public Page<Expense> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(ExpenseMapper::toDomain);
    }

}
