package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExpenseDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ExpenseMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Expense;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import com.moriba.skultem.domain.repository.ExpenseRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateExpenseUseCase {
    private final ExpenseRepository repo;
    private final ExpenseCategoryRepository expenseCategoryRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "EXPENSE_CREATED")
    public ExpenseDTO execute(String schoolId, String name, String description, String categoryId, BigDecimal amount, LocalDate expenseDate) {

        var category = expenseCategoryRepo.findByIdAndSchool(categoryId, schoolId).orElseThrow(() -> new NotFoundException("Category not found"));
        var id = UUID.randomUUID().toString();

        var domain = Expense.create(id, schoolId, name, amount, category, description, expenseDate);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.EXPENSE,
                "Expense created",
                domain.getTitle(),
                null,
                domain.getId());

        return ExpenseMapper.toDTO(domain);
    }
}
