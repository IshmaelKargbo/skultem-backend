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
        private final com.moriba.skultem.domain.repository.AssessmentCaEntryRepository caEntryRepo;
        private final com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository cycleRepo;
        private final com.moriba.skultem.application.services.AssessmentStructureService structureService;

        public List<StudentAssessmentDTO> execute(String schoolId, String teacherSubjectId, String termId) {

                var studentAssessments = repo.findAllByTeacherSubjectIdTermId(teacherSubjectId, termId);

                var scoresByStudent = loadScores(studentAssessments);

                // A safety net: an assessment that is open but hasn't frozen its structure yet freezes now -
                // then the scores are read again so every row sees the frozen structure.
                var cycles = scoresByStudent.values().stream().flatMap(List::stream).map(AssessmentScore::getCycle)
                                .distinct().toList();
                var frozen = structureService.freezeOpened(cycles);
                if (!frozen.isEmpty()) {
                        cycleRepo.saveAll(frozen);
                        scoresByStudent = loadScores(studentAssessments);
                }

                // The continuous-assessment recordings, for the students whose assessment uses them.
                var scoreIds = scoresByStudent.values().stream().flatMap(List::stream)
                                .filter(s -> s.getCycle().isContinuous()).map(AssessmentScore::getId).toList();
                var entriesByScore = caEntryRepo.findAllByScoreIds(scoreIds).stream()
                                .collect(java.util.stream.Collectors.groupingBy(e -> e.getAssessmentScoreId()));

                final var scores = scoresByStudent;
                return studentAssessments.stream()
                                .map(sa -> StudentAssessmentMapper.toDTO(sa, scores.get(sa.getId()), entriesByScore))
                                .toList();
        }

        private java.util.Map<String, List<AssessmentScore>> loadScores(
                        List<com.moriba.skultem.domain.model.StudentAssessment> studentAssessments) {
                var map = new java.util.LinkedHashMap<String, List<AssessmentScore>>();
                for (var sa : studentAssessments) {
                        map.put(sa.getId(), assessmentScoreRepo.findAllByStudentAssessment(sa.getId()));
                }
                return map;
        }
}
