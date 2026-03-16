package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCycleDTO;
import com.moriba.skultem.application.dto.AssessmentDTO;
import com.moriba.skultem.application.mapper.AssessmentCycleMapper;
import com.moriba.skultem.application.mapper.AssessmentMapper;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAssessmentUseCase {

        private final ClassSubjectAssessmentLifeCycleRepository repo;
        private final AssessmentRepository assessmentRepo;

        public List<AssessmentCycleDTO> execute(String schoolId, String subjectId, String termId) {
                return repo.findAllBySubjectAndTerm(subjectId, termId).stream()
                                .map(AssessmentCycleMapper::toDTO)
                                .toList();
        }

        public List<AssessmentDTO> executeAssessment(String schoolId) {
                return assessmentRepo.findAllBySchoolId(schoolId).stream()
                                .map(AssessmentMapper::toDTO)
                                .toList();
        }
}
