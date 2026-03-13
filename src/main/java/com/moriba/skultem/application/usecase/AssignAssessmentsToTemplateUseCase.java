package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AssessmentMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignAssessmentsToTemplateUseCase {
    private final AssessmentTemplateRepository templateRepo;
    private final AssessmentRepository assessmentRepo;

    @AuditLogAnnotation(action = "ASSIGNED_ASSESSMENT_TO_TEMPLATE")
    public AssessmentTemplateDTO execute(String schoolId, String templateId, List<AssessmentInput> assignments) {
        var template = templateRepo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));

        var sanitized = sanitize(assignments);

        double totalWeight = sanitized.stream().mapToDouble(AssessmentInput::weight).sum();
        if (totalWeight > 100.0) {
            throw new RuleException("Total assessment weight cannot exceed 100%");
        }

        assessmentRepo.deleteAllByTemplateIdAndSchoolId(templateId, schoolId);

        List<Assessment> records = new ArrayList<>();
        int position = 1;
        for (AssessmentInput assignment : sanitized) {
            records.add(Assessment.create(UUID.randomUUID().toString(), schoolId, template, assignment.name(),
                    assignment.weight(), position));
            position++;
        }

        assessmentRepo.saveAll(records);

        var list = records.stream().map(AssessmentMapper::toDTO).toList();
        return new AssessmentTemplateDTO(template.getId(), template.getName(), template.getDescription(), list,
                template.getCreatedAt(), template.getUpdatedAt());
    }

    private List<AssessmentInput> sanitize(List<AssessmentInput> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            throw new RuleException("At least one assessment assignment is required");
        }

        List<AssessmentInput> sanitized = new ArrayList<>();
        HashSet<String> uniqueNames = new HashSet<>();

        for (AssessmentInput assignment : assignments) {
            if (assignment == null || assignment.name() == null || assignment.name().isBlank()) {
                throw new RuleException("Assessment name is required");
            }
            if (assignment.weight() == null || assignment.weight() <= 0 || assignment.weight() > 100) {
                throw new RuleException("Each assessment weight must be greater than 0 and not exceed 100");
            }

            var cleanName = assignment.name().trim();
            var key = cleanName.toLowerCase(Locale.ROOT);
            if (!uniqueNames.add(key)) {
                throw new RuleException("Duplicate assessment name: " + cleanName);
            }

            sanitized.add(new AssessmentInput(cleanName, assignment.weight()));
        }

        return sanitized;
    }

    public record AssessmentInput(String name, Integer weight) {
    }
}
