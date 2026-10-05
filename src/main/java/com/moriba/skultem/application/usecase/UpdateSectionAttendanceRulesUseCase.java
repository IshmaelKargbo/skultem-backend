package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Sets (or clears) one management section's own attendance rules. A null value means the section
// uses the school's again - see AttendanceRulesResolver.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSectionAttendanceRulesUseCase {

    public record Input(Double attendanceThreshold, Integer attendanceWindowDays, Integer attendanceMinDays,
            Integer attendanceStreakDays) {
    }

    private final SchoolRepository schoolRepo;
    private final ManagementSectionRepository sectionRepo;
    private final GetSchoolStructureUseCase getSchoolStructureUseCase;

    public SchoolStructureDTO execute(String schoolId, String sectionId, Input in) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            throw new RuleException("This school isn't managed in sections");
        }
        var section = sectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Management section not found"));

        section.updateAttendanceRules(in.attendanceThreshold(), in.attendanceWindowDays(), in.attendanceMinDays(),
                in.attendanceStreakDays());
        sectionRepo.save(section);

        return getSchoolStructureUseCase.execute(schoolId);
    }
}
