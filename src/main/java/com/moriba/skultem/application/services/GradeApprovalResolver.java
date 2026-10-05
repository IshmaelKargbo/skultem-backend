package com.moriba.skultem.application.services;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.vo.GradeApprover;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Works out who approves grades for a level: the class master or an admin. The school sets a default
// and a management section (primary vs secondary often differ) can override it; a school without
// sections, or a level with no section / no override, uses the school's. Same lookup shape as
// AttendanceRulesResolver.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeApprovalResolver {

    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository managementSectionRepo;

    public GradeApprover forLevel(School school, Level level) {
        if (level == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return school.getGradeApprover();
        }
        return forAllLevels(school).getOrDefault(level, school.getGradeApprover());
    }

    // Only the levels whose section sets its own choice are in the map; anything else is the school's.
    public Map<Level, GradeApprover> forAllLevels(School school) {
        Map<Level, GradeApprover> result = new EnumMap<>(Level.class);
        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return result;
        }
        Map<String, GradeApprover> bySection = new HashMap<>();
        for (var s : managementSectionRepo.findBySchoolId(school.getId())) {
            if (s.getGradeApprover() != null) {
                bySection.put(s.getId(), s.getGradeApprover());
            }
        }
        for (var l : schoolLevelRepo.findBySchoolId(school.getId())) {
            var approver = l.getManagementSectionId() == null ? null : bySection.get(l.getManagementSectionId());
            if (approver != null) {
                result.put(l.getLevel(), approver);
            }
        }
        return result;
    }
}
