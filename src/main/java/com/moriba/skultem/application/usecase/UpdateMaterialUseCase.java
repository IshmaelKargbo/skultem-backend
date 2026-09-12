package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.MaterialMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Material.Unit;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateMaterialUseCase {
    private final MaterialRepository repo;
    private final MaterialCategoryRepository categoryRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_UPDATED")
    public MaterialDTO execute(String schoolId, String id, String name, Unit unit, BigDecimal price,
            String categoryId) {
        var material = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Material not found"));

        if (!material.getName().equalsIgnoreCase(name) && repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Material already exists");
        }

        var category = categoryRepo.findByIdAndSchool(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("category not found"));

        material.update(name, unit, category, price);
        repo.save(material);

        logActivityUseCase.log(schoolId, ActivityType.FEES, "Material updated",
                material.getName(), null, material.getId());

        return MaterialMapper.toDTO(material);
    }
}
