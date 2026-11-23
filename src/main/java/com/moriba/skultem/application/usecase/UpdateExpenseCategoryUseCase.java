package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExpenseCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ExpenseCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Renames an expense category or changes its description. Expenses under it follow, since they point at its id. */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateExpenseCategoryUseCase {
    private final ExpenseCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "EXPENSE_CATEGORY_UPDATED")
    public ExpenseCategoryDTO execute(String schoolId, String id, String name, String description) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new RuleException("Name is required");
        }
        var category = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Expense category not found"));
        if (repo.existsByNameAndSchoolExcept(cleanName, schoolId, id)) {
            throw new AlreadyExistsException("Another expense category is already called " + cleanName);
        }
        category.update(cleanName, description == null ? null : description.trim());
        repo.save(category);

        logActivityUseCase.log(schoolId, ActivityType.EXPENSE, "Expense category updated", cleanName, null, id);
        return ExpenseCategoryMapper.toDTO(category);
    }
}
