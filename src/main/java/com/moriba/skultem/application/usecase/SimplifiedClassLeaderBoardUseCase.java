package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.application.dto.BreakdownDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.mapper.AssessmentScoreMapper;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SimplifiedClassLeaderBoardUseCase {

        private final AssessmentScoreRepository repo;
        private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

        public Page<BreakdownDTO> execute(ReportBuilderDTO request, int page, int size) {

                Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();
                List<Filter> filters = request.filters();

                List<AssessmentScore> scores = repo
                                .runReport(request.schoolId(), filters, Pageable.unpaged())
                                .getContent();

                List<BreakdownDTO> breakdown = new ArrayList<>();

                // Group scores by subject
                Map<String, List<AssessmentScore>> scoresBySubject = scores.stream()
                                .collect(Collectors.groupingBy(
                                                s -> s.getCycle().getSubject().getId()));

                for (var subjectEntry : scoresBySubject.entrySet()) {

                        List<AssessmentScore> subjectScores = subjectEntry.getValue();

                        // Sort by assessment position
                        subjectScores.sort(Comparator.comparingInt(
                                        a -> a.getCycle().getAssessment().getPosition()));

                        int cumulativeWeight = 0;
                        String trend = "STABLE";
                        Integer previousScore = null;

                        List<AssessmentScoreDTO> scoresDTO = new ArrayList<>();

                        for (AssessmentScore score : subjectScores) {
                                if (!score.isApproved())
                                        continue;

                                int currentScore = score.getScore() != null ? score.getScore() : 0;
                                int currentWeight = score.getWeightedScore() != null ? score.getWeightedScore() : 0;

                                if (previousScore != null) {
                                        if (currentScore > previousScore)
                                                trend = "IMPROVED";
                                        else if (currentScore < previousScore)
                                                trend = "DROPPED";
                                }

                                previousScore = currentScore;
                                cumulativeWeight += currentWeight;

                                String grade = resolveScoreGradeUseCase.execute(request.schoolId(), score.getScore(),
                                                score.getStatus());
                                
                                scoresDTO.add(AssessmentScoreMapper.toDTO(score, grade, trend));
                        }

                        if (subjectScores.isEmpty())
                                continue;

                        AssessmentScore lastScore = subjectScores.get(subjectScores.size() - 1);

                        String grade = resolveScoreGradeUseCase.execute(request.schoolId(), cumulativeWeight,
                                        lastScore.getStatus());

                        breakdown.add(new BreakdownDTO(
                                        lastScore.getStudentAssessment().getTeacherSubject().getId(),
                                        lastScore.getStudentAssessment().getTeacherSubject().getSubject().getName(),
                                        cumulativeWeight,
                                        grade,
                                        trend,
                                        scoresDTO));
                }

                // Sort by cumulative score descending
                breakdown.sort(Comparator.comparingInt(BreakdownDTO::score).reversed());

                // Pagination
                int start = (page - 1) * size;
                int end = Math.min(start + size, breakdown.size());
                List<BreakdownDTO> pageContent = (size > 0 && start < breakdown.size())
                                ? breakdown.subList(start, end)
                                : breakdown;

                return new PageImpl<>(pageContent, pageable, breakdown.size());
        }
}