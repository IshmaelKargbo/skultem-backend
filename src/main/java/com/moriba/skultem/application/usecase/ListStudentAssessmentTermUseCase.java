package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentAssessmentDTO;
import com.moriba.skultem.application.mapper.StudentAssessmentMapper;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentAssessmentTermUseCase {

        private final StudentAssessmentRepository repo;
        private final AssessmentScoreRepository assessmentScoreRepo;

        public List<StudentAssessmentDTO> execute(String schoolId, String teacherSubjectId, String termId) {

                var studentAssessments = repo.findAllByTeacherSubjectIdTermId(teacherSubjectId, termId);

                return studentAssessments.stream()
                                .map(sa -> {
                                        List<AssessmentScore> scores = assessmentScoreRepo
                                                        .findAllByStudentAssessment(sa.getId());

                                        return StudentAssessmentMapper.toDTO(sa, scores);
                                })
                                .toList();
        }
}
