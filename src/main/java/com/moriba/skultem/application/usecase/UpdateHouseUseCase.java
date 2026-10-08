package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HouseDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HouseMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateHouseUseCase {
    private final HouseRepository repo;
    private final TeacherRepository teacherRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "HOUSE_UPDATED")
    public HouseDTO execute(String schoolId, String id, String name, String motto, String color,
            List<String> masters) {
        var house = repo.findByIdAndSchool(id, schoolId).orElseThrow(() -> new NotFoundException("House not found"));

        if (!house.getName().equalsIgnoreCase(name) && repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("House already exists");
        }

        List<Teacher> houseMasters = masters.stream()
                .map(teacherId -> teacherRepo.findById(teacherId)
                        .orElseThrow(() -> new NotFoundException("Teacher not found: " + teacherId)))
                .toList();

        house.update(name, motto, color, houseMasters);
        repo.save(house);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "House updated", house.getName(), null, house.getId());

        return HouseMapper.toDTO(house);
    }
}
