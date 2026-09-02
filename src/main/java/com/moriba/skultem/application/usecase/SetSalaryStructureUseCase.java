package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SalaryStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SalaryStructureMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SalaryStructure;
import com.moriba.skultem.domain.repository.SalaryStructureRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A teacher has at most one salary structure - setting one for a teacher who already has one
// updates it in place rather than erroring, so the "Add Salary" and "Edit Salary" flows on the
// frontend can share one call.
@Service
@Transactional
@RequiredArgsConstructor
public class SetSalaryStructureUseCase {
    private final SalaryStructureRepository repo;
    private final TeacherRepository teacherRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SALARY_STRUCTURE_SET")
    public SalaryStructureDTO execute(String schoolId, String teacherId, BigDecimal basicSalary,
            BigDecimal allowances, BigDecimal deductions) {

        var existing = repo.findByTeacherIdAndSchoolId(teacherId, schoolId);

        SalaryStructure structure;

        if (existing.isPresent()) {
            structure = existing.get();
            structure.update(basicSalary, allowances, deductions);
        } else {
            var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Teacher not found"));

            structure = SalaryStructure.create(UUID.randomUUID().toString(), schoolId, teacher, basicSalary,
                    allowances, deductions);
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
