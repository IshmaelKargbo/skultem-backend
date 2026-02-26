package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAcadamicYearUseCase {

    private final AcademicYearRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public AcademicYearDTO execute(String school, String name, LocalDate startDate, LocalDate endDate) {
        if (repo.existsByNameAndSchool(name, school)) {
            throw new AlreadyExistsException("academic year already exist");
        }

        var id = rg.generate("ACADEMIC_YEAR", "ACY");
        var academicYear = AcademicYear.create(id, school, name, startDate, endDate);

        var countRes = repo.findAllBySchool(school, Pageable.unpaged());
        if (countRes.getSize() == 0) {
            academicYear.setActive(true);
        }

        repo.save(academicYear);

        return AcademicYearMapper.toDTO(academicYear);
    }
}
