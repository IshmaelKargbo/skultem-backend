package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.MaterialCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateMaterialCategoryUseCase {
    private final MaterialCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_CATEGORY_UPDATED")
    public MaterialCategoryDTO execute(String schoolId, String categoryId, String name, String description) {
        var category = repo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Material category not found"));

        if (!category.getName().equalsIgnoreCase(name) && repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Material category already exists");
        }

        category.update(name, description);
        repo.save(category);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Material category updated",
                category.getName(),
                null,
                category.getId());

        return MaterialCategoryMapper.toDTO(category);
    }
}
