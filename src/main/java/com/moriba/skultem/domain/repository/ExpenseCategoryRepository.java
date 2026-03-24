package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ExpenseCategory;

public interface ExpenseCategoryRepository {
    void save(ExpenseCategory domain);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<ExpenseCategory> findByIdAndSchool(String id, String schoolId);

    Page<ExpenseCategory> findBySchool(String schoolId, Pageable pageable);
}
