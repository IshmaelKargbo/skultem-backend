package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.MaterialMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.domain.model.Material.Unit;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateMaterialUseCase {
    private final MaterialRepository repo;
    private final MaterialCategoryRepository categoryRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_CREATED")
    public MaterialDTO execute(String schoolId, String name, Unit unit, int qty, String categoryId) {
        if (repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Material already exists");
        }

        var category = categoryRepo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("category not found"));
        var domain = Material.create(schoolId, name, unit, qty, category);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Material category created",
                domain.getName(),
                null,
                domain.getId());

        return MaterialMapper.toDTO(domain);
    }
}
