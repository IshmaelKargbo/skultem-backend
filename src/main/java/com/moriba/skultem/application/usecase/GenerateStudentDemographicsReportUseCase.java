package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReligionCountDTO;
import com.moriba.skultem.application.dto.StudentDemographicsDTO;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Student demographics (gender/religion) counted straight from Student + Enrollment records - no
// duplicate demographic fields, no fabricated categories. Religion is free text on Student, so it
// is bucketed into the school's four reporting categories (Muslim/Christian/Other/Not specified)
// here by keyword match on the raw value rather than trusting staff to have entered it
// consistently; every value that isn't blank and doesn't match Muslim/Christian falls into "Other"
// rather than being invented a new bucket.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateStudentDemographicsReportUseCase {

    private static final List<String> RELIGION_LABELS = List.of("Muslim", "Christian", "Other", "Not specified");

    private final EnrollmentRepository enrollmentRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final SectionScopeService sectionScopeService;

    public StudentDemographicsDTO execute(String schoolId, String academicYearId, String classId, Level level) {
        // A missing academicYearId resolves to the school's active year (never "every year at
        // once") - the same convention every other academic report on this feature follows, so a
        // student who has one enrollment per year isn't double-counted across their whole history.
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var rows = enrollmentRepo.demographicsByFilters(schoolId, academicYear.getId(), classId, level,
                sectionScopeService.levels());

        int boys = 0;
        int girls = 0;
        Map<String, Long> religionCounts = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Gender gender = (Gender) row[0];
            String religion = (String) row[1];
            long count = ((Number) row[2]).longValue();

            if (gender == Gender.MALE) {
                boys += count;
            } else if (gender == Gender.FEMALE) {
                girls += count;
            }

            religionCounts.merge(bucketReligion(religion), count, Long::sum);
        }

        List<ReligionCountDTO> religions = RELIGION_LABELS.stream()
                .map(label -> new ReligionCountDTO(label, religionCounts.getOrDefault(label, 0L)))
                .toList();

        return new StudentDemographicsDTO(boys + girls, boys, girls, 0, religions);
    }

    private String bucketReligion(String religion) {
        if (religion == null || religion.isBlank()) {
            return "Not specified";
        }

        String normalized = religion.trim().toLowerCase();
        if (normalized.contains("muslim") || normalized.contains("islam")) {
            return "Muslim";
        }
        if (normalized.contains("christian")) {
            return "Christian";
        }
        return "Other";
    }
}
