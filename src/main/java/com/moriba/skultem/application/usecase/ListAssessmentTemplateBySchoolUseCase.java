package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.application.mapper.AssessmentMapper;
import com.moriba.skultem.application.mapper.AssessmentTemplateMapper;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.repository.AssessmentConfigurationRepository;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAssessmentTemplateBySchoolUseCase {
    private final AssessmentTemplateRepository templateRepo;
    private final AssessmentRepository assessmentRepo;
    private final AssessmentConfigurationRepository configRepo;

    public Page<AssessmentTemplateDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var templates = templateRepo.findAllBySchoolId(schoolId, pageable);
        var configs = configRepo.findAllBySchool(schoolId);
        var schoolWide = configs.stream()
                .filter(c -> c.getAssessmentTemplateId() == null && c.getManagementSectionId() == null).findFirst()
                .orElse(null);

        return templates.map(template -> {
            var assessments = assessmentRepo.findAllByTemplateIdAndSchoolId(template.getId(), schoolId).stream()
                    .map(AssessmentMapper::toDTO)
                    .toList();
            var own = configs.stream().filter(c -> template.getId().equals(c.getAssessmentTemplateId())).findFirst()
                    .orElse(null);
            return AssessmentTemplateMapper.toDTO(template, assessments,
                    scoringOf(own != null ? own : schoolWide, own == null && schoolWide != null));
        });
    }

    private static AssessmentTemplateDTO.Scoring scoringOf(AssessmentConfiguration c, boolean inherited) {
        if (c == null || !c.isContinuous()) {
            return AssessmentTemplateDTO.Scoring.simple();
        }
        // A template's own setup lists the tests that use CA, each with its own parts.
        var caPlan = c.getPlan().stream().filter(e -> e.usesCa()).toList();
        if (c.getAssessmentTemplateId() != null) {
            if (caPlan.isEmpty()) {
                return AssessmentTemplateDTO.Scoring.simple();
            }
            return new AssessmentTemplateDTO.Scoring(c.getStructure().name(), 0, 100,
                    caPlan.stream().mapToInt(e -> e.components().size()).max().orElse(0),
                    caPlan.stream().anyMatch(e -> e.components().stream().anyMatch(x -> x.fromAttendance())),
                    c.getPlan().size(), false, caPlan.size());
        }
        return new AssessmentTemplateDTO.Scoring(c.getStructure().name(), c.getCaPercentage(), c.getFormalPercentage(),
                c.getComponents().size(), c.getComponents().stream().anyMatch(x -> x.fromAttendance()),
                c.getPlan().size(), inherited, 0);
    }
}
