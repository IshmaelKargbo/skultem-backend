package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTermUseCase {

    private final AcademicYearRepository academicYearRepo;
    private final TermRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public TermDTO execute(String schoolId, String academicYearId, String name, LocalDate startDate,
            LocalDate endDate) {

        AcademicYear year = academicYearRepo.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (startDate.isBefore(year.getStartDate())) {
            throw new IllegalArgumentException("Term start date cannot be before academic year start date");
        }
        if (endDate.isAfter(year.getEndDate())) {
            throw new IllegalArgumentException("Term end date cannot be after academic year end date");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Term start date must be before term end date");
        }

        var existingTerms = repo.findByAcademicYearIdAndSchool(academicYearId, schoolId);

        if (existingTerms.size() >= 3) {
            throw new AlreadyExistsException("Cannot create more than 3 terms for this academic year");
        }

        int termNumber = existingTerms.stream()
                .map(Term::getTermNumber)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        existingTerms.forEach(existingTerm -> {
            if (!endDate.isBefore(existingTerm.getStartDate()) && !startDate.isAfter(existingTerm.getEndDate())) {
                throw new IllegalArgumentException(
                        "Term dates overlap with existing term: " + existingTerm.getName());
            }

            if (termNumber > existingTerm.getTermNumber() && !startDate.isAfter(existingTerm.getEndDate())) {
                throw new IllegalArgumentException(
                        "Term start date must be after the end of previous term: " + existingTerm.getName());
            }
        });

        var id = rg.generate("TERM", "TRM");
        var term = Term.create(id, schoolId, year, name, termNumber, Term.Status.UPCOMING, startDate,
                endDate);
        repo.save(term);

        return TermMapper.toDTO(term);
    }
}
