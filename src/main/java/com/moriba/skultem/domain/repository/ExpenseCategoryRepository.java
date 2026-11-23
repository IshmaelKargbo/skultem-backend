package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ExpenseCategory;

public interface ExpenseCategoryRepository {
    void save(ExpenseCategory domain);

    void delete(ExpenseCategory domain);

    // Another category of the school already has this name (case ignored).
    boolean existsByNameAndSchoolExcept(String name, String schoolId, String exceptId);

    // Expenses recorded under this category.
    long countExpenses(String categoryId, String schoolId);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<ExpenseCategory> findByIdAndSchool(String id, String schoolId);

    Page<ExpenseCategory> findBySchool(String schoolId, Pageable pageable);

    Page<ExpenseCategory> search(String schoolId, String query, Pageable pageable);
}
