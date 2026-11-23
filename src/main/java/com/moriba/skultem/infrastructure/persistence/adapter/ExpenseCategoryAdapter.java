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
    public void delete(ExpenseCategory domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public boolean existsByNameAndSchoolExcept(String name, String schoolId, String exceptId) {
        return repo.existsByNameIgnoreCaseAndSchoolIdAndIdNot(name, schoolId, exceptId);
    }

    @Override
    public long countExpenses(String categoryId, String schoolId) {
        return repo.countExpenses(categoryId, schoolId);
    }

    @Override
    public Optional<ExpenseCategory> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ExpenseCategoryMapper::toDomain);
    }

    @Override
    public Page<ExpenseCategory> search(String schoolId, String query, Pageable pageable) {
        return repo.search(schoolId, query, pageable).map(ExpenseCategoryMapper::toDomain);
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
