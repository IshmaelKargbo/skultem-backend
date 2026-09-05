package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SalaryStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PayComponentItemMapper;
import com.moriba.skultem.application.mapper.SalaryStructureMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SalaryStructure;
import com.moriba.skultem.domain.repository.SalaryStructureRepository;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.infrastructure.rest.dto.PayComponentItemDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A teacher has at most one salary structure - setting one for a teacher who already has one
// updates it in place rather than erroring, so the "Add Salary" and "Edit Salary" flows on the
// frontend can share one call.
//
// templateId is purely for display/traceability (see SalaryStructure) - it does NOT drive the
// saved allowances/deductions here. The frontend resolves a picked template into concrete line
// items itself (fetch the template, prefill the item builder, let the admin tweak it) and always
// submits the final list either way, whether it started from a template or was built from
// scratch - so this use case only needs to look the template up long enough to resolve its
// current name for that display purpose, never to "apply" it.
@Service
@Transactional
@RequiredArgsConstructor
public class SetSalaryStructureUseCase {
    private final SalaryStructureRepository repo;
    private final TeacherRepository teacherRepo;
    private final SalaryTemplateRepository templateRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SALARY_STRUCTURE_SET")
    public SalaryStructureDTO execute(String schoolId, String teacherId, String templateId, BigDecimal basicSalary,
            List<PayComponentItemDTO> allowances, List<PayComponentItemDTO> deductions) {

        String templateName = null;
        if (templateId != null && !templateId.isBlank()) {
            templateName = templateRepo.findByIdAndSchoolId(templateId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Salary template not found"))
                    .getName();
        } else {
            templateId = null;
        }

        var resolvedAllowances = PayComponentItemMapper.toDomain(allowances);
        var resolvedDeductions = PayComponentItemMapper.toDomain(deductions);

        var existing = repo.findByTeacherIdAndSchoolId(teacherId, schoolId);

        SalaryStructure structure;

        if (existing.isPresent()) {
            structure = existing.get();
            structure.update(templateId, templateName, basicSalary, resolvedAllowances, resolvedDeductions);
        } else {
            var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Teacher not found"));

            structure = SalaryStructure.create(UUID.randomUUID().toString(), schoolId, teacher, templateId,
                    templateName, basicSalary, resolvedAllowances, resolvedDeductions);
        }

        repo.save(structure);

        logActivityUseCase.log(
                schoolId,
                ActivityType.PAYMENT,
                "Salary structure set",
                structure.getTeacher().getUser().getName(),
                null,
                structure.getId());

        return SalaryStructureMapper.toDTO(structure);
    }
}
