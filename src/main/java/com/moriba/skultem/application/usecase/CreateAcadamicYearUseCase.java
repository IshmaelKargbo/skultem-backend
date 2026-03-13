package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAcadamicYearUseCase {

    private final AcademicYearRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

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

        logActivityUseCase.log(
                school,
                ActivityType.SCHOOL,
                "Academic year created",
                academicYear.getName() + " (" + academicYear.getStartDate() + " - " + academicYear.getEndDate() + ")",
                null,
                academicYear.getId());

        return AcademicYearMapper.toDTO(academicYear);
    }
}
