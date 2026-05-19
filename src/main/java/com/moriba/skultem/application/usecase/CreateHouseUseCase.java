package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HouseDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HouseMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateHouseUseCase {
    private final HouseRepository repo;
    private final TeacherRepository teacherRepos;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "HOUSE_CREATED")
    public HouseDTO execute(String schoolId, String name, String motto, String color,
            String master) {
        if (repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("House already exists");
        }

        Teacher houseMaster = null;

        if (!master.isBlank()) {
            houseMaster = teacherRepos.findById(schoolId)
                    .orElseThrow(() -> new NotFoundException("Teacher not found"));
        }

        var id = UUID.randomUUID().toString();
        var holiday = House.create(id, schoolId, name, motto, color, houseMaster);
        repo.save(holiday);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "House created",
                holiday.getName() + " (" + holiday.getMotto() + ")",
                null,
                holiday.getId());

        return HouseMapper.toDTO(holiday);
    }
}
