package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HolidayDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HolidayMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Holiday;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateHolidayUseCase {
    private final HolidayRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final ReferenceGeneratorUsecase rg;

    public HolidayDTO execute(String schoolId, String name, LocalDate date, Holiday.Kind kind, boolean fixed) {
        if (repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Holiday already exists");
        }

        AcademicYear academicYear = academicYearRepo.findActiveBySchool(schoolId)
                    .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (kind.equals(Holiday.Kind.PUBLIC)) {
            fixed = true;
        }

        var id = rg.generate("HOLIDAY", "HOD");
        var holiday = Holiday.create(id, schoolId, name, date, kind, academicYear, fixed);
        repo.save(holiday);
        return HolidayMapper.toDTO(holiday);
    }
}
