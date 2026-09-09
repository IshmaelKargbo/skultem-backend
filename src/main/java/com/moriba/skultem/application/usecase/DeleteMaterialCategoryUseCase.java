package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a material category the school doesn't want. Blocked once at least one material is
 * assigned to it - a category backing materials already tracked in inventory isn't something this
 * removes out from under them; those materials have to be recategorized or removed first.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteMaterialCategoryUseCase {

    private final MaterialCategoryRepository repo;
    private final MaterialRepository materialRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_CATEGORY_DELETED")
    public void execute(String schoolId, String categoryId) {
        var category = repo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Material category not found"));

        if (materialRepo.existsByCategoryAndSchool(categoryId, schoolId)) {
            throw new RuleException(
                    "This category is used by one or more materials and can't be deleted. Recategorize or remove those materials first.");
        }

        repo.deleteById(categoryId);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Material category deleted",
                category.getName(),
                null,
                categoryId);
    }
}
