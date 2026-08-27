package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.FeeCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateFeeCategoryUseCase {
    private final FeeCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_CATEGORY_UPDATED")
    public FeeCategoryDTO execute(String schoolId, String categoryId, String name, String description) {
        var category = repo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Fee category not found"));

        // Managed automatically by SeedPlatformFeeForAcademicYearUseCase, which finds it again every
        // year by this exact name - renaming or redescribing it here would desync that lookup.
        if (category.getName().equals(SeedPlatformFeeForAcademicYearUseCase.PLATFORM_FEE_CATEGORY_NAME)) {
            throw new RuleException("The Platform Fee category is managed automatically and can't be edited");
        }

        if (!category.getName().equalsIgnoreCase(name) && repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Fee category already exists");
        }

        category.update(name, description);
        repo.save(category);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Fee category updated",
                category.getName(),
                null,
                category.getId());

        return FeeCategoryMapper.toDTO(category);
    }
}
