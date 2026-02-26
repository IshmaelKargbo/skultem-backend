package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HolidayDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HolidayMapper;
import com.moriba.skultem.domain.model.Holiday.Kind;
import com.moriba.skultem.domain.repository.HolidayRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateHolidayUseCase {
    private final HolidayRepository repo;

    public HolidayDTO execute(String schoolId, String id, String name, LocalDate date, Kind kind, boolean fixed) {
        var holiday = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Holiday not found"));

        if (!holiday.getName().equalsIgnoreCase(name) && repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Holiday already exists");
        }

        holiday.update(name, kind, date, fixed);
        repo.save(holiday);
        return HolidayMapper.toDTO(holiday);
    }
}
