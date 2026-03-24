package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Expense;

public interface ExpenseRepository {
    void save(Expense domain);

    Optional<Expense> findByIdAndSchool(String id, String schoolId);

    Page<Expense> findBySchool(String schoolId, Pageable pageable);
}
