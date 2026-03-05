package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.application.mapper.AssessmentMapper;
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

    public Page<AssessmentTemplateDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var templates = templateRepo.findAllBySchoolId(schoolId, pageable);

        return templates.map(template -> {
            var assessments = assessmentRepo.findAllByTemplateIdAndSchoolId(template.getId(), schoolId).stream()
                    .map(AssessmentMapper::toDTO)
                    .toList();
            return new AssessmentTemplateDTO(template.getId(), template.getName(), template.getDescription(),
                    assessments, template.getCreatedAt(), template.getUpdatedAt());
        });
    }
}
