package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ExpenseCategory;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ExpenseCategoryJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ExpenseCategoryMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExpenseCategoryAdapter implements ExpenseCategoryRepository {
    private final ExpenseCategoryJpaRepository repo;

    @Override
    public void save(ExpenseCategory domain) {
        var entity = ExpenseCategoryMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<ExpenseCategory> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ExpenseCategoryMapper::toDomain);
    }

    @Override
    public Page<ExpenseCategory> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(ExpenseCategoryMapper::toDomain);
    }

    @Override
    public boolean existByNameAndSchoolId(String name, String schoolid) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolid);
    }

}
