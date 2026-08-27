package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a fee category the school doesn't want. Blocked once at least one fee structure uses it
 * - a category backing a fee students may already be carrying charges under isn't something this
 * removes out from under them; the fee structure(s) using it have to be dealt with first.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteFeeCategoryUseCase {

    private final FeeCategoryRepository repo;
    private final FeeStructureRepository feeStructureRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_CATEGORY_DELETED")
    public void execute(String schoolId, String categoryId) {
        var category = repo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Fee category not found"));

        if (category.getName().equals(SeedPlatformFeeForAcademicYearUseCase.PLATFORM_FEE_CATEGORY_NAME)) {
            throw new RuleException("The Platform Fee category is managed automatically and can't be deleted");
        }

        if (feeStructureRepo.existsByCategoryAndSchool(categoryId, schoolId)) {
            throw new RuleException(
                    "This fee category is used by one or more fee structures and can't be deleted. Remove those fee structures first.");
        }

        repo.deleteById(categoryId);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Fee category deleted",
                category.getName(),
                null,
                categoryId);
    }
}
