package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateClassTemplateUseCase {

    private final ClassRepository classRepository;
    private final AssessmentTemplateRepository assessmentTemplateRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;

    public ClassDTO execute(String schoolId, String classId, String templateId) {
        var clazz = classRepository.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (assessmentScoreRepository.existsGradeActivityByClassIdAndSchoolId(classId, schoolId)) {
            throw new RuleException("Class template cannot be changed because grading has already started for this class");
        }

        var template = assessmentTemplateRepository.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));

        clazz.setTemplate(template);
        classRepository.save(clazz);

        return ClassMapper.toDTO(clazz);
    }
}
