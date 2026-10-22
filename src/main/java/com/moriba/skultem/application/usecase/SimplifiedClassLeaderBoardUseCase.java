package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

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
        private final SectionScopeService sectionScopeService;
        private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

        public Page<BreakdownDTO> execute(ReportBuilderDTO request, int page, int size) {

                Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();
                List<Filter> filters = request.filters();

                List<AssessmentScore> scores = repo
                                .runReport(request.schoolId(), filters, sectionScopeService.levels(), Pageable.unpaged())
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

                        // Only marked, approved work counts - a draft, submitted or never-marked (locked at 0) test
                        // has no score yet and must not read as a drop to 0.
                        List<AssessmentScore> approved = subjectScores.stream()
                                        .filter(AssessmentScore::hasSettledScore).toList();

                        List<AssessmentScoreDTO> scoresDTO = new ArrayList<>();
                        Integer previousScore = null;
                        String trend = null;
                        long weightedSum = 0;
                        int weightTotal = 0;
                        int rawSum = 0;

                        for (AssessmentScore score : approved) {
                                int currentScore = score.getScore() != null ? score.getScore() : 0;
                                int weight = score.getWeight() != null ? score.getWeight() : 0;

                                // Each row's trend is against the test before it; the subject's is its latest one.
                                trend = previousScore == null ? null
                                                : currentScore > previousScore ? "IMPROVED"
                                                                : currentScore < previousScore ? "DROPPED" : "STABLE";
                                previousScore = currentScore;

                                weightedSum += (long) currentScore * weight;
                                weightTotal += weight;
                                rawSum += currentScore;

                                String grade = resolveScoreGradeUseCase.execute(request.schoolId(), currentScore);
                                scoresDTO.add(AssessmentScoreMapper.toDTO(score, grade, trend));
                        }

                        AssessmentScore lastScore = subjectScores.get(subjectScores.size() - 1);
                        int passMark = lastScore.getAssessment().getTemplate().getPassMark();

                        // Weighted percentage over what has been approved so far: a 30% test scored 80 reads as 80,
                        // not 24. Falls back to a plain average if those tests carry no weight.
                        Integer subjectScore = null;
                        String grade = null;
                        Boolean passed = null;
                        if (!approved.isEmpty()) {
                                subjectScore = weightTotal > 0
                                                ? (int) Math.round((double) weightedSum / weightTotal)
                                                : Math.round((float) rawSum / approved.size());
                                grade = resolveScoreGradeUseCase.execute(request.schoolId(), subjectScore);
                                passed = subjectScore >= passMark;
                        }

                        breakdown.add(new BreakdownDTO(
                                        lastScore.getStudentAssessment().getTeacherSubject().getId(),
                                        lastScore.getStudentAssessment().getTeacherSubject().getSubject().getName(),
                                        subjectScore,
                                        grade,
                                        // One marked test so far (e.g. the first test, exam still to come): nothing
                                        // to compare against yet, so it is stable - not a drop.
                                        approved.isEmpty() ? null : trend == null ? "STABLE" : trend,
                                        scoresDTO,
                                        passed,
                                        passMark,
                                        approved.size(),
                                        subjectScores.size()));
                }

                // Highest first; subjects with nothing approved yet go last.
                breakdown.sort(Comparator.comparing(BreakdownDTO::score,
                                Comparator.nullsLast(Comparator.<Integer>reverseOrder())));

                // Pagination
                int start = (page - 1) * size;
                int end = Math.min(start + size, breakdown.size());
                List<BreakdownDTO> pageContent = (size > 0 && start < breakdown.size())
                                ? breakdown.subList(start, end)
                                : breakdown;

                return new PageImpl<>(pageContent, pageable, breakdown.size());
        }
}