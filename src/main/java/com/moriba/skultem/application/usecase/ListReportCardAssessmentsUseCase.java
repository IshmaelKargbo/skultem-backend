package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardAssessmentOptionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListReportCardAssessmentsUseCase {
    private final ClassRepository classRepo;
    private final AssessmentRepository assessmentRepo;

    public List<ReportCardAssessmentOptionDTO> execute(String schoolId, String classId) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (clazz.getTemplate() == null) {
            return List.of();
        }

        return assessmentRepo.findAllByTemplateIdAndSchoolId(clazz.getTemplate().getId(), schoolId).stream()
                .sorted(Comparator.comparingInt(Assessment::getPosition))
                .map(e -> new ReportCardAssessmentOptionDTO(e.getId(), e.getName(), e.getWeight(), e.getPosition()))
                .toList();
    }
}
