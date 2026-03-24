package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
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

    public Page<AssessmentScoreDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();
        List<Filter> filters = request.filters();

        List<AssessmentScore> scores = repo.runReport(request.schoolId(), filters, Pageable.unpaged())
                                           .getContent();

        // 1️⃣ Group scores by student
        Map<String, List<AssessmentScore>> scoresByStudent = scores.stream()
                .collect(Collectors.groupingBy(s -> s.getStudentAssessment().getEnrollment().getStudent().getId()));

        List<AssessmentScoreDTO> leaderboard = new ArrayList<>();
        Map<String, Double> studentAverageMap = new HashMap<>();

        for (var studentEntry : scoresByStudent.entrySet()) {
            String studentId = studentEntry.getKey();
            List<AssessmentScore> studentScores = studentEntry.getValue();

            // 2️⃣ Sort assessments by level (ascending: Test1 → Test2 → Exam)
            studentScores.sort(Comparator.comparingInt(a -> a.getCycle().getAssessment().getPosition()));

            double cumulativeScore = 0;
            double cumulativeWeight = 0;
            double previousAverage = 0;
            String lastTrend = "NEUTRAL";

            for (AssessmentScore a : studentScores) {
                if (!a.isCompleted()) continue;

                cumulativeScore += a.getWeightedScore();
                cumulativeWeight += a.getWeight();
                double currentAverage = cumulativeWeight > 0 ? cumulativeScore / cumulativeWeight : 0;

                // 3️⃣ Calculate trend based on progression
                if (previousAverage == 0) lastTrend = "IMPROVED";
                else if (currentAverage > previousAverage) lastTrend = "IMPROVED";
                else if (currentAverage < previousAverage) lastTrend = "DROPPED";
                else lastTrend = "NEUTRAL";

                previousAverage = currentAverage;
            }

            double average = cumulativeWeight > 0 ? cumulativeScore / cumulativeWeight : 0;
            studentAverageMap.put(studentId, average);

            AssessmentScore lastScore = studentScores.isEmpty() ? null : studentScores.get(studentScores.size() - 1);

            // 4️⃣ Create parent-friendly summary DTO (no subject details)
            AssessmentScoreDTO dto = AssessmentScoreMapper.toDTO(
                    lastScore,
                    "", // grade placeholder
                    lastTrend,
                    average
            );
            leaderboard.add(dto);
        }

        // 5️⃣ Sort students descending by average for class rank
        leaderboard.sort((a, b) -> Double.compare(studentAverageMap.get(b.student()),
                                                  studentAverageMap.get(a.student())));

        // 6️⃣ Pagination
        int start = (page - 1) * size;
        int end = Math.min(start + size, leaderboard.size());
        List<AssessmentScoreDTO> pageContent = (size > 0 && start < leaderboard.size())
                ? leaderboard.subList(start, end)
                : leaderboard;

        return new PageImpl<>(pageContent, pageable, leaderboard.size());
    }
}