package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SupplyMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateSupplyUseCase {
    private final SupplyRepository repo;
    private final MaterialRepository materialRepo;
    private final StudentRepository studentRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUPPLY_CREATED")
    public SupplyDTO execute(String schoolId, String studentId, String materialId, int qty) {
        if (repo.existsByStudentIdAndMaterialIdAndSchoolId(studentId, materialId, schoolId)) {
            throw new AlreadyExistsException("Supply already exists");
        }

        var material = materialRepo.findByIdAndSchool(materialId, schoolId)
                .orElseThrow(() -> new NotFoundException("material not found"));

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("student not found"));

        var domain = Supply.create(schoolId, student, material, qty);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Supply category created",
                student.getName() + " to collect " + qty + " " + material.getName(),
                null,
                domain.getId());

        return SupplyMapper.toDTO(domain);
    }
}
