package com.moriba.skultem.application.services;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Rules;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Works out which attendance rules (threshold, window, minimum days, absence streak) apply to a
// level. A SECTION_BASED school can let each management section override any of them (primary and
// secondary often differ); a value the section leaves blank - and every level in a UNIFIED school, or
// one with no section - uses the school's own. Same lookup shape as SchoolBrandingResolver.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRulesResolver {

    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository managementSectionRepo;

    public Rules forLevel(School school, Level level) {
        if (level == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return school.attendanceRules();
        }
        return forAllLevels(school).getOrDefault(level, school.attendanceRules());
    }

    // Every offered level at once - use this when a use case spans many classes.
    public Map<Level, Rules> forAllLevels(School school) {
        Map<Level, Rules> result = new EnumMap<>(Level.class);
        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return result;
        }
        Map<String, ManagementSection> sections = new java.util.HashMap<>();
        for (var s : managementSectionRepo.findBySchoolId(school.getId())) {
            sections.put(s.getId(), s);
        }
        for (SchoolLevel l : schoolLevelRepo.findBySchoolId(school.getId())) {
            var section = l.getManagementSectionId() == null ? null : sections.get(l.getManagementSectionId());
            if (section != null) {
                result.put(l.getLevel(), merge(school, section));
            }
        }
        return result;
    }

    public static Rules merge(School school, ManagementSection section) {
        var base = school.attendanceRules();
        int window = section.getAttendanceWindowDays() != null ? section.getAttendanceWindowDays()
                : base.windowDays();
        int min = section.getAttendanceMinDays() != null ? section.getAttendanceMinDays() : base.minDays();
        return new Rules(
                section.getAttendanceThreshold() != null ? section.getAttendanceThreshold() : base.threshold(),
                window,
                // The minimum can never exceed the window actually in force, whichever level set each.
                Math.min(min, window),
                section.getAttendanceStreakDays() != null ? section.getAttendanceStreakDays() : base.streakDays());
    }
}
