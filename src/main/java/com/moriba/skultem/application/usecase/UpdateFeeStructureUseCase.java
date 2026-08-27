package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Lets an admin adjust a fee structure's details - most usefully, the ones a new academic year just
 * copied forward from last year (see {@link ConfigureNextAcademicYearUseCase}) that need this year's
 * new amount/due date before they're used. What it applies to (academic year, class, type) isn't
 * editable - see {@link com.moriba.skultem.domain.model.FeeStructure#update}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateFeeStructureUseCase {

    private final FeeStructureRepository repo;
    private final FeeCategoryRepository feeCategoryRepo;
    private final TermRepository termRepo;
    private final MaterialRepository materialRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_STRUCTURE_UPDATED")
    public FeeStructureDTO execute(UpdateRecord param) {
        var fee = repo.findByIdAndSchoolId(param.feeId(), param.schoolId())
                .orElseThrow(() -> new NotFoundException("Fee structure not found"));

        var category = feeCategoryRepo.findByIdAndSchool(param.feeCategory(), param.schoolId())
                .orElseThrow(() -> new NotFoundException("Fee category not found"));

        var term = termRepo.findByIdAndAcademicYearIdAndSchoolId(param.termId(), fee.getAcademicYear().getId(),
                param.schoolId())
                .orElseThrow(() -> new NotFoundException("Term not found"));

        Material material = null;
        if (param.hasSupply() && param.materialId() != null) {
            material = materialRepo.findByIdAndSchool(param.materialId(), param.schoolId())
                    .orElseThrow(() -> new NotFoundException("Material not found"));
        }

        fee.update(term, category, material, param.hasSupply(), param.totalSupply(), param.dueDate(), param.amount(),
                param.description(), param.allowInstallment());

        repo.save(fee);

        logActivityUseCase.log(
                param.schoolId(),
                ActivityType.FEES,
                "Fee structure updated",
                category.getName() + " - " + term.getName(),
                null,
                fee.getId());

        return FeeStructureMapper.toDTO(fee);
    }

    public record UpdateRecord(
            String schoolId,
            String feeId,
            String feeCategory,
            String termId,
            String materialId,
            BigDecimal amount,
            LocalDate dueDate,
            boolean allowInstallment,
            String description,
            boolean hasSupply,
            int totalSupply) {
    }
}
