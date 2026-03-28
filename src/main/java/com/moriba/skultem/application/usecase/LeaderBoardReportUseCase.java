package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LeaderBoardAssessmentDTO;
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

        public Page<LeaderBoardDTO> execute(ReportBuilderDTO request, int page, int size) {

                List<Filter> filters = request.filters();

                // Fetch all scores for the school + filters
                List<AssessmentScore> scores = repo
                                .runReport(request.schoolId(), filters, Pageable.unpaged())
                                .getContent();

                // Group scores by student
                Map<String, List<AssessmentScore>> scoresByStudent = scores.stream()
                                .filter(AssessmentScore::isCompleted)
                                .collect(Collectors.groupingBy(
                                                s -> s.getStudentAssessment()
                                                                .getEnrollment()
                                                                .getStudent()
                                                                .getId()));

                List<LeaderBoardDTO> leaderboard = new ArrayList<>();

                for (var entry : scoresByStudent.entrySet()) {

                        List<AssessmentScore> studentScores = entry.getValue();
                        if (studentScores.isEmpty())
                                continue;

                        // Sort by assessment position (cycle order)
                        studentScores.sort(Comparator.comparingInt(
                                        s -> s.getCycle().getAssessment().getPosition()));

                        AssessmentScore latest = studentScores.get(studentScores.size() - 1);
                        String studentId = latest.getStudentAssessment()
                                        .getEnrollment()
                                        .getStudent()
                                        .getId();
                        String studentName = latest.getStudentAssessment()
                                        .getEnrollment()
                                        .getStudent()
                                        .getName();

                        // -----------------------------
                        // Group scores by assessment cycle (term) and sum totals
                        // -----------------------------
                        Map<Integer, List<AssessmentScore>> scoresByCycle = studentScores.stream()
                                        .collect(Collectors.groupingBy(
                                                        s -> s.getCycle().getAssessment().getPosition(), // classifier
                                                        LinkedHashMap::new, // map supplier
                                                        Collectors.toList() // downstream collector
                                        ));

                        List<LeaderBoardAssessmentDTO> assessments = scoresByCycle.entrySet().stream()
                                        .map(entryCycle -> {
                                                List<AssessmentScore> cycleScores = entryCycle.getValue();

                                                double totalScore = cycleScores.stream()
                                                                .mapToDouble(AssessmentScore::getScore)
                                                                .sum();

                                                double totalWeightedScore = cycleScores.stream()
                                                                .mapToDouble(AssessmentScore::getWeightedScore)
                                                                .sum();

                                                String assessmentName = cycleScores.get(0).getCycle().getAssessment()
                                                                .getName();
                                                String assessmentId = cycleScores.get(0).getCycle().getAssessment()
                                                                .getId();

                                                return new LeaderBoardAssessmentDTO(
                                                                assessmentId,
                                                                assessmentName,
                                                                (int) totalScore,
                                                                cycleScores.get(0).getCycle().getAssessment()
                                                                                .getWeight(),
                                                                cycleScores.get(0).getCycle().getAssessment()
                                                                                .getPosition(),
                                                                (int) totalWeightedScore);
                                        })
                                        .sorted(Comparator.comparingInt(a -> a.position())) // optional: sort by cycle
                                        .toList();

                        // -----------------------------
                        // Total weighted score for the leaderboard
                        // -----------------------------
                        int totalWeightedScore = assessments.stream()
                                        .mapToInt(LeaderBoardAssessmentDTO::weightedScore)
                                        .sum();

                        int finalScore = assessments.stream()
                                        .mapToInt(LeaderBoardAssessmentDTO::score)
                                        .sum();

                        String trend = "STABLE";
                        if (assessments.size() >= 2) {
                                double previousTotal = assessments.get(assessments.size() - 2).score();
                                double currentTotal = assessments.get(assessments.size() - 1).score();

                                if (currentTotal > previousTotal)
                                        trend = "IMPROVED";
                                else if (currentTotal < previousTotal)
                                        trend = "DROPPED";
                        }

                        leaderboard.add(new LeaderBoardDTO(
                                        studentId,
                                        0, // rank will be set later
                                        studentName,
                                        finalScore,
                                        totalWeightedScore,
                                        trend,
                                        assessments));
                }

                // Sort leaderboard by total score descending
                leaderboard.sort(Comparator.comparingInt(LeaderBoardDTO::score).reversed());

                // Assign ranks (handling ties)
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

                        leaderboard.set(i, new LeaderBoardDTO(
                                        lb.id(),
                                        rank,
                                        lb.name(),
                                        lb.score(),
                                        lb.weight(),
                                        lb.trend(),
                                        lb.assessments()));
                }

                // Pagination
                if (size <= 0) {
                        return new PageImpl<>(leaderboard, Pageable.unpaged(), leaderboard.size());
                }

                int start = Math.min((page - 1) * size, leaderboard.size());
                int end = Math.min(start + size, leaderboard.size());

                List<LeaderBoardDTO> pageContent = leaderboard.subList(start, end);

                return new PageImpl<>(pageContent, PageRequest.of(page - 1, size), leaderboard.size());
        }
}