package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.MaterialCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialCategory;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateMaterialCategoryUseCase {
    private final MaterialCategoryRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_CATEGORY_CREATED")
    public MaterialCategoryDTO execute(String schoolId, String name, String description) {
        if (repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Material category already exists");
        }

        var domain = MaterialCategory.create(schoolId, name, description);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Material category created",
                domain.getName(),
                null,
                domain.getId());

        return MaterialCategoryMapper.toDTO(domain);
    }
}
