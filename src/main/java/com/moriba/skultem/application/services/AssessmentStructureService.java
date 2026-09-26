package com.moriba.skultem.application.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.AssessmentConfigurationRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Which assessment configuration applies, and freezing it onto an assessment when the assessment opens.
//   effective configuration = the level's management section's own, else the school-wide one, else SIMPLE
// (exactly how assessments have always worked - so a school that never touches this sees no change).
@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentStructureService {

    private final AssessmentConfigurationRepository configRepo;
    private final SchoolRepository schoolRepo;
    private final SchoolLevelRepository schoolLevelRepo;

    public AssessmentConfiguration forSection(String schoolId, String sectionId) {
        if (sectionId != null) {
            var own = configRepo.findBySection(schoolId, sectionId);
            if (own.isPresent()) {
                return own.get();
            }
        }
        return configRepo.findBySection(schoolId, null)
                .orElseGet(() -> AssessmentConfiguration.simple(schoolId, sectionId));
    }

    public AssessmentConfiguration forLevel(String schoolId, Level level) {
        return forSection(schoolId, sectionOfLevel(schoolId, level));
    }

    private String sectionOfLevel(String schoolId, Level level) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (level == null || school == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return null;
        }
        return schoolLevelRepo.findBySchoolId(schoolId).stream()
                .filter(l -> l.getLevel() == level)
                .map(SchoolLevel::getManagementSectionId)
                .filter(id -> id != null)
                .findFirst().orElse(null);
    }

    // Freezes the configuration in force now onto every assessment that is OPEN (drafting or returned) and has
    // not been frozen yet. Assessments that are still locked wait until they open; ones already frozen keep
    // the structure and weights they were opened with. Returns those it changed, for the caller to save.
    public java.util.List<ClassSubjectAssessmentLifeCycle> freezeOpened(
            Collection<ClassSubjectAssessmentLifeCycle> cycles) {
        Map<String, AssessmentConfiguration> byLevel = new HashMap<>();
        var changed = new java.util.ArrayList<ClassSubjectAssessmentLifeCycle>();
        for (var cycle : cycles) {
            boolean open = cycle.getStatus() == Status.DRAFT || cycle.getStatus() == Status.RETURNED;
            if (!open || cycle.isStructureFrozen()) {
                continue;
            }
            Level level = cycle.getSubject().getSession().getClazz().getLevel();
            var config = byLevel.computeIfAbsent(cycle.getSchoolId() + "|" + level,
                    k -> forLevel(cycle.getSchoolId(), level));
            cycle.freezeStructure(config);
            changed.add(cycle);
        }
        return changed;
    }

    // The levels one management section covers, in a SECTION_BASED school - null (every level) for a
    // UNIFIED school or a null sectionId, matching how forSection/forLevel treat "no section" as
    // "whole school". See RefreshUnstartedAssessmentsUseCase.
    public java.util.Set<Level> levelsOfSection(String schoolId, String sectionId) {
        if (sectionId == null) {
            return null;
        }
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return null;
        }
        return schoolLevelRepo.findBySchoolId(schoolId).stream()
                .filter(l -> sectionId.equals(l.getManagementSectionId()))
                .map(SchoolLevel::getLevel)
                .collect(java.util.stream.Collectors.toSet());
    }
}
