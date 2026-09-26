package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.AssessmentConfigurationDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.AssessmentConfigurationRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// The assessment configuration of each management section (or the whole school when it isn't run in
// sections), as the caller may see it: everyone who works with assessments can READ it - teachers need it to
// know how to record - but a section-limited caller only sees their own sections'.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListAssessmentConfigurationsUseCase {

    private final SchoolRepository schoolRepo;
    private final ManagementSectionRepository sectionRepo;
    private final AssessmentConfigurationRepository configRepo;
    private final UserRepository userRepo;
    private final SectionScopeService sectionScopeService;

    public List<AssessmentConfigurationDTO> execute(String schoolId) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        var stored = configRepo.findAllBySchool(schoolId);
        var schoolWide = stored.stream().filter(c -> c.getManagementSectionId() == null).findFirst().orElse(null);

        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return List.of(toDTO(schoolWide != null ? schoolWide : AssessmentConfiguration.simple(schoolId, null),
                    null, false));
        }

        var scope = sectionScopeService.currentOrAll();
        return sectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> scope.wholeSchool() || scope.sectionIds().contains(s.getId()))
                .map(section -> {
                    var own = stored.stream().filter(c -> section.getId().equals(c.getManagementSectionId()))
                            .findFirst().orElse(null);
                    if (own != null) {
                        return toDTO(own, section.getName(), false);
                    }
                    var fallback = schoolWide != null ? schoolWide
                            : AssessmentConfiguration.simple(schoolId, section.getId());
                    return toDTO(fallback, section.getName(), schoolWide != null, section.getId());
                })
                .toList();
    }

    public AssessmentConfigurationDTO toDTO(AssessmentConfiguration c, String sectionName, boolean inherited) {
        return toDTO(c, sectionName, inherited, c.getManagementSectionId());
    }

    private AssessmentConfigurationDTO toDTO(AssessmentConfiguration c, String sectionName, boolean inherited,
            String sectionId) {
        String by = c.getUpdatedByUserId() == null ? null
                : userRepo.findById(c.getUpdatedByUserId())
                        .map(u -> u.getGivenNames() + " " + u.getFamilyName()).orElse(null);
        return new AssessmentConfigurationDTO(sectionId, sectionName, c.getStructure(), c.isContinuous(),
                c.getCaPercentage(), c.getFormalPercentage(), c.getCaFrequency(),
                c.getCaFrequency() == null ? null : c.getCaFrequency().getUnit(), c.getCaEntries(), c.getVersion(),
                c.isDefault(), inherited, c.isDefault() ? null : c.getUpdatedAt(), by,
                c.getPlan().stream().map(e -> new AssessmentConfigurationDTO.PlanItem(e.termId(), e.assessmentName(),
                        e.usesCa(), e.caEntries())).toList());
    }
}
