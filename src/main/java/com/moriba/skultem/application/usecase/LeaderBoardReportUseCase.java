package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LeaderBoardDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LeaderBoardReportUseCase {

        private final AssessmentScoreRepository repo;
        private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

        public Page<LeaderBoardDTO> execute(ReportBuilderDTO request, int page, int size) {

                List<Filter> filters = request.filters();

                List<AssessmentScore> scores = repo.runReport(request.schoolId(), filters, Pageable.unpaged())
                                .getContent();

                Map<String, List<AssessmentScore>> scoresByStudent = scores.stream()
                                .filter(AssessmentScore::isCompleted)
                                .collect(Collectors.groupingBy(
                                                s -> s.getStudentAssessment().getEnrollment().getStudent().getId()));

                List<LeaderBoardDTO> leaderboard = new ArrayList<>();
                for (var entry : scoresByStudent.entrySet()) {
                        List<AssessmentScore> studentScores = entry.getValue();
                        if (studentScores.isEmpty())
                                continue;

                        studentScores.sort(Comparator.comparingInt(s -> s.getCycle().getAssessment().getPosition()));

                        double totalWeightedScore = studentScores.stream()
                                        .mapToDouble(AssessmentScore::getScore)
                                        .sum();
                        int totalWeight = studentScores.stream()
                                        .mapToInt(s -> s.getCycle().getAssessment().getWeight())
                                        .sum();
                        int finalScore = totalWeight > 0 ? (int) Math.round((totalWeightedScore / totalWeight) * 100)
                                        : 0;

                        String trend = "STABLE";
                        if (studentScores.size() >= 2) {
                                double last = studentScores.get(studentScores.size() - 1).getWeightedScore();
                                double prev = studentScores.get(studentScores.size() - 2).getWeightedScore();
                                trend = last > prev ? "IMPROVED" : (last < prev ? "DROPPED" : "STABLE");
                        }

                        AssessmentScore latest = studentScores.get(studentScores.size() - 1);
                        String studentId = latest.getStudentAssessment().getEnrollment().getStudent().getId();
                        String studentName = latest.getStudentAssessment().getEnrollment().getStudent().getName();
                        String grade = resolveScoreGradeUseCase.execute(request.schoolId(), finalScore,
                                        latest.getStatus());

                        leaderboard.add(new LeaderBoardDTO(studentId, 0, studentName, finalScore, totalWeight, grade,
                                        trend));
                }

                leaderboard.sort(Comparator.comparingInt(LeaderBoardDTO::score).reversed());

                int rank = 1;
                int previousScore = -1;
                int sameRankCount = 0;
                for (int i = 0; i < leaderboard.size(); i++) {
                        LeaderBoardDTO lb = leaderboard.get(i);
                        if (lb.score() == previousScore) {
                                sameRankCount++;
                        } else {
                                rank += sameRankCount;
                                sameRankCount = 1;
                        }
                        previousScore = lb.score();
                        leaderboard.set(i, new LeaderBoardDTO(lb.id(), rank, lb.name(), lb.score(), lb.weight(),
                                        lb.grade(), lb.trend()));
                }

                if (size <= 0) {
                        return new PageImpl<>(leaderboard, Pageable.unpaged(), leaderboard.size());
                }
                int start = Math.min((page - 1) * size, leaderboard.size());
                int end = Math.min(start + size, leaderboard.size());
                List<LeaderBoardDTO> pageContent = leaderboard.subList(start, end);

                return new PageImpl<>(pageContent, PageRequest.of(page - 1, size), leaderboard.size());
        }
}