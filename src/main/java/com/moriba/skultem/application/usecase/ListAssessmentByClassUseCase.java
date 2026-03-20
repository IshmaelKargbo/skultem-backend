package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AssessmentMapper;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAssessmentByClassUseCase {

        private final ClassRepository repo;
        private final AssessmentRepository assessmentRepo;

        public List<AssessmentDTO> execute(String schoolId, String classId) {
                var clazz = repo.findByIdAndSchool(classId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));
                var template = clazz.getTemplate();

                return assessmentRepo.findAllByTemplateIdAndSchoolId(template.getId(), schoolId)
                                .stream().map(AssessmentMapper::toDTO)
                                .toList();
        }
}
