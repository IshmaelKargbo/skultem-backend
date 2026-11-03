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

    // The levels whose grades go to this approver - lets a queue show only what one approver acts on.
    public java.util.Set<Level> levelsRoutedTo(School school, GradeApprover approver) {
        var result = java.util.EnumSet.noneOf(Level.class);
        var overrides = forAllLevels(school);
        for (var l : schoolLevelRepo.findBySchoolId(school.getId())) {
            if (overrides.getOrDefault(l.getLevel(), school.getGradeApprover()) == approver) {
                result.add(l.getLevel());
            }
        }
        return result;
    }

    public com.moriba.skultem.application.dto.GradeApprovalRoutingDTO routing(School school) {
        var routes = new java.util.ArrayList<com.moriba.skultem.application.dto.GradeApprovalRoutingDTO.Route>();
        var levels = schoolLevelRepo.findBySchoolId(school.getId());
        boolean sectionBased = school.getManagementModel() == ManagementModel.SECTION_BASED;

        if (!sectionBased) {
            routes.add(new com.moriba.skultem.application.dto.GradeApprovalRoutingDTO.Route(null, "Whole school",
                    levels.stream().map(l -> l.getLevel()).sorted().toList(), school.getGradeApprover(), false));
        } else {
            var sections = managementSectionRepo.findBySchoolId(school.getId()).stream()
                    .sorted(java.util.Comparator.comparingInt(s -> s.getDisplayOrder())).toList();
            for (var s : sections) {
                var own = levels.stream().filter(l -> s.getId().equals(l.getManagementSectionId()))
                        .map(l -> l.getLevel()).sorted().toList();
                var approver = s.getGradeApprover() != null ? s.getGradeApprover() : school.getGradeApprover();
                routes.add(new com.moriba.skultem.application.dto.GradeApprovalRoutingDTO.Route(s.getId(),
                        s.getName(), own, approver, s.getGradeApprover() != null));
            }
            var loose = levels.stream().filter(l -> l.getManagementSectionId() == null).map(l -> l.getLevel())
                    .sorted().toList();
            if (!loose.isEmpty()) {
                routes.add(new com.moriba.skultem.application.dto.GradeApprovalRoutingDTO.Route(null, "Other levels",
                        loose, school.getGradeApprover(), false));
            }
        }
        return new com.moriba.skultem.application.dto.GradeApprovalRoutingDTO(school.getGradeApprover(),
                sectionBased, routes);
    }
}
