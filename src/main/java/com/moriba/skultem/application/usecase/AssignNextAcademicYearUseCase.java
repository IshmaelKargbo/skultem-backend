package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Links an academic year the school already created as the "next" year for the current one - the
 * lighter counterpart to {@link ConfigureNextAcademicYearUseCase} for schools that set up next
 * year's own dates themselves and just need to point the current year at it. This link is what
 * unlocks promotion ({@link ValidateNextAcademicYearUseCase}) and, later, closing the year
 * ({@link CloseAcademicYearAndActivateNextUseCase}).
 * <p>
 * Also templates next year's terms from this year's (see {@link TemplateTermsForAcademicYearUseCase})
 * if it doesn't have any yet - a school that created the year record itself very likely hasn't set up
 * its terms either, and without this they'd have no term to activate once the year opens.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AssignNextAcademicYearUseCase {

    private final AcademicYearRepository academicYearRepo;
    private final TemplateTermsForAcademicYearUseCase templateTermsForAcademicYearUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ACADEMIC_YEAR_NEXT_ASSIGNED")
    public AcademicYearDTO execute(String schoolId, String currentYearId, String nextYearId) {
        var currentYear = academicYearRepo.findByIdAndSchoolId(currentYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (currentYearId.equals(nextYearId)) {
            throw new RuleException("An academic year can't be its own next year");
        }

        var nextYear = academicYearRepo.findByIdAndSchoolId(nextYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Selected next academic year not found"));

        if (!nextYear.getStartDate().isAfter(currentYear.getEndDate())) {
            throw new RuleException(nextYear.getName() + " must start after " + currentYear.getName() + " ends");
        }

        // Promotion/close-year resolve "next year" by date, not this link (see findNextBySchool) - if
        // some other year already sits chronologically between the two, linking this one would be
        // misleading since that earlier year is what actually gets used.
        academicYearRepo.findNextBySchool(schoolId, currentYear.getEndDate())
                .filter(existing -> !existing.getId().equals(nextYear.getId()))
                .ifPresent(existing -> {
                    throw new RuleException(existing.getName() + " already immediately follows " + currentYear.getName()
                            + " - assign that year instead, or adjust dates so " + nextYear.getName() + " comes first.");
                });

        currentYear.setNext(nextYear);
        academicYearRepo.save(currentYear);

        var createdTerms = templateTermsForAcademicYearUseCase.execute(schoolId, currentYear, nextYear);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Next academic year assigned",
                nextYear.getName() + " assigned as the year following " + currentYear.getName()
                        + (createdTerms.isEmpty() ? "" : " with " + createdTerms.size() + " term(s) templated"),
                null,
                nextYear.getId());

        return AcademicYearMapper.toDTO(currentYear);
    }
}
