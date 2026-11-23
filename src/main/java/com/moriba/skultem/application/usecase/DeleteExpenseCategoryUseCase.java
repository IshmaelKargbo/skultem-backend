package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Deletes an expense category no expense is recorded under - typically one created by mistake. */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteExpenseCategoryUseCase {
    private final ExpenseCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "EXPENSE_CATEGORY_DELETED")
    public void execute(String schoolId, String id) {
        var category = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Expense category not found"));

        long expenses = repo.countExpenses(id, schoolId);
        if (expenses > 0) {
            throw new RuleException(category.getName() + " has " + expenses + " expense" + (expenses == 1 ? "" : "s")
                    + " recorded under it, so it can't be deleted. Rename it instead if the name is wrong.");
        }

        repo.delete(category);
        logActivityUseCase.log(schoolId, ActivityType.EXPENSE, "Expense category deleted", category.getName(), null, id);
    }
}
