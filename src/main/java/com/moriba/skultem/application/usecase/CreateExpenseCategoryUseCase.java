package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExpenseCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.ExpenseCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ExpenseCategory;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateExpenseCategoryUseCase {
    private final ExpenseCategoryRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "EXPENSE_CATEGORY_CREATED")
    public ExpenseCategoryDTO execute(String schoolId, String name, String description) {
        if (repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Expense category already exists");
        }

        var id = rg.generate("EXPENSE_CATEGORY", "EXC");
        var domain = ExpenseCategory.create(id, schoolId, name, description);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.EXPENSE,
                "Expense category created",
                domain.getName(),
                null,
                domain.getId());

        return ExpenseCategoryMapper.toDTO(domain);
    }
}
