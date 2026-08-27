package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.dto.AssignHouseRecord;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignHouseUseCase {

    private final StudentRepository repo;
    private final HouseRepository houseRepo;

    @AuditLogAnnotation(action = "HOUSE_ASSIGNED")
    @Transactional
    public void execute(List<AssignHouseRecord> records, String schoolId) {
        records.forEach(e -> {
            var domain = repo.findByIdAndSchoolId(e.id(), schoolId).orElseThrow(() -> new NotFoundException("Student not found"));
            var house = houseRepo.findByIdAndSchool(e.house(), schoolId).orElseThrow(() -> new NotFoundException("House not found"));
            domain.assignHouse(house);
            repo.save(domain);
        });
    }

}
