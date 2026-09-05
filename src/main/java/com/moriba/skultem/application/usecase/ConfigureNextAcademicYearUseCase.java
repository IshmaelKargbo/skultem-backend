package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;

import com.moriba.skultem.application.dto.ConfigureNextAcademicYearResultDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.FeeStructureSupplyItem;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfigureNextAcademicYearUseCase {

    private static final Pattern YEAR_RANGE = Pattern.compile("(\\d{4})(\\D+)(\\d{4})");
    private static final Pattern SINGLE_YEAR = Pattern.compile("(\\d{4})");

    private final AcademicYearRepository academicYearRepo;
    private final FeeStructureRepository feeStructureRepo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;
    private final TemplateTermsForAcademicYearUseCase templateTermsForAcademicYearUseCase;

    @AuditLogAnnotation(action = "ACADEMIC_YEAR_NEXT_CONFIGURED")
    public ConfigureNextAcademicYearResultDTO execute(String schoolId, String currentYearId, String name,
            LocalDate startDate, LocalDate endDate) {

        var currentYear = academicYearRepo.findByIdAndSchoolId(currentYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (currentYear.getNextYear() != null
                || academicYearRepo.findNextBySchool(schoolId, currentYear.getEndDate()).isPresent()) {
            throw new RuleException("Next academic year is already configured for " + currentYear.getName());
        }

        LocalDate nextStart = startDate != null ? startDate : currentYear.getStartDate().plusYears(1);
        LocalDate nextEnd = endDate != null ? endDate : currentYear.getEndDate().plusYears(1);

        if (!nextStart.isAfter(currentYear.getEndDate())) {
            throw new RuleException("Next academic year must start after " + currentYear.getName() + " ends");
        }
        if (!nextEnd.isAfter(nextStart)) {
            throw new RuleException("End date must be after start date");
        }

        String nextName = (name != null && !name.isBlank()) ? name.trim() : deriveNextName(currentYear.getName());

        if (academicYearRepo.existsByNameAndSchool(nextName, schoolId)) {
            throw new AlreadyExistsException("academic year already exist");
        }

        var yearId = rg.generate("ACADEMIC_YEAR", "ACY");
        var nextYear = AcademicYear.create(yearId, schoolId, nextName, nextStart, nextEnd);
        academicYearRepo.save(nextYear);

        currentYear.setNext(nextYear);
        academicYearRepo.save(currentYear);

        var createdTerms = templateTermsForAcademicYearUseCase.execute(schoolId, currentYear, nextYear);
        var copiedFees = copyFeeStructures(schoolId, currentYear, nextYear, createdTerms);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Next academic year configured",
                nextYear.getName() + " set up from " + currentYear.getName()
                        + (createdTerms.isEmpty() ? "" : " with " + createdTerms.size() + " term(s) templated")
                        + (copiedFees == 0 ? "" : " and " + copiedFees + " fee structure(s) copied"),
                null,
                nextYear.getId());

        return new ConfigureNextAcademicYearResultDTO(
                AcademicYearMapper.toDTO(nextYear),
                createdTerms.stream().map(TermMapper::toDTO).toList(),
                copiedFees);
    }

    private int copyFeeStructures(String schoolId, AcademicYear currentYear, AcademicYear nextYear,
            List<Term> nextYearTerms) {
        var sourceFees = feeStructureRepo.findBySchoolAndAcademic(schoolId, currentYear.getId(), Pageable.unpaged())
                .getContent();

        if (sourceFees.isEmpty()) {
            return 0;
        }

        Map<Integer, Term> nextTermByNumber = nextYearTerms.stream()
                .collect(Collectors.toMap(a -> a.getTermNumber(), t -> t, (a, b) -> a));

        int copied = 0;

        for (var sourceFee : sourceFees) {
            // The platform fee isn't copied forward - SeedPlatformFeeForAcademicYearUseCase creates a
            // fresh, correctly-flagged one for the new academic year once its first term activates.
            if (sourceFee.isSystem()) {
                continue;
            }

            Term nextTerm = sourceFee.getTerm() != null ? nextTermByNumber.get(sourceFee.getTerm().getTermNumber())
                    : null;

            if (sourceFee.getTerm() != null && nextTerm == null) {
                continue;
            }

            LocalDate dueDate = sourceFee.getDueDate() != null ? sourceFee.getDueDate().plusYears(1) : null;

            // Fresh ids for the copy's own supply items - they belong to this new FeeStructure row,
            // not shared with the source fee's.
            List<FeeStructureSupplyItem> supplyItems = sourceFee.getSupplyItems() == null
                    ? List.of()
                    : sourceFee.getSupplyItems().stream()
                            .map(item -> new FeeStructureSupplyItem(java.util.UUID.randomUUID().toString(),
                                    item.getMaterial(), item.getQuantity()))
                            .toList();

            var copy = FeeStructure.create(
                    schoolId,
                    sourceFee.getType(),
                    sourceFee.getClazz(),
                    sourceFee.isHasSupply(),
                    supplyItems,
                    nextTerm,
                    sourceFee.getCategory(),
                    nextYear,
                    dueDate,
                    sourceFee.getAmount(),
                    sourceFee.getDescription(),
                    sourceFee.isAllowInstallment(),
                    sourceFee.isNewStudentsOnly(),
                    sourceFee.isOldStudentsOnly(),
                    sourceFee.getGender());

            feeStructureRepo.save(copy);
            copied++;
        }

        return copied;
    }

    private String deriveNextName(String currentName) {
        Matcher range = YEAR_RANGE.matcher(currentName);
        if (range.find()) {
            int start = Integer.parseInt(range.group(1)) + 1;
            int end = Integer.parseInt(range.group(3)) + 1;
            return currentName.substring(0, range.start(1)) + start + range.group(2) + end
                    + currentName.substring(range.end(3));
        }

        Matcher single = SINGLE_YEAR.matcher(currentName);
        if (single.find()) {
            int year = Integer.parseInt(single.group(1)) + 1;
            return currentName.substring(0, single.start(1)) + year + currentName.substring(single.end(1));
        }

        return currentName + " (Next)";
    }
}
