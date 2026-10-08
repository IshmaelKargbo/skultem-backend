package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteHouseUseCase {
    private final HouseRepository repo;
    private final StudentRepository studentRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "HOUSE_DELETED")
    public void execute(String schoolId, String id) {
        var house = repo.findByIdAndSchool(id, schoolId).orElseThrow(() -> new NotFoundException("House not found"));

        if (studentRepo.existsByHouseId(id)) {
            throw new RuleException("Students are still assigned to this house - reassign them before deleting it");
        }

        repo.delete(house);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "House deleted", house.getName(), null, house.getId());
    }
}
