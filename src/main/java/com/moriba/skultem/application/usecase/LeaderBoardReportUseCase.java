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
public class LeaderBoardReportUseCase {

    private final AssessmentScoreRepository repo;
    private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

    public Page<AssessmentScoreDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0)
                ? PageRequest.of(page - 1, size)
                : Pageable.unpaged();

        List<Filter> filters = request.filters();

        Page<AssessmentScore> res =
                repo.runReport(request.schoolId(), filters, pageable);

        List<AssessmentScore> scores = res.getContent();

        // Group scores by student
        Map<String, List<AssessmentScore>> scoresByStudent =
                scores.stream().collect(Collectors.groupingBy(
                        s -> s.getStudentAssessment()
                                .getEnrollment()
                                .getStudent()
                                .getId()
                ));

        List<AssessmentScoreDTO> leaderboard = new ArrayList<>();

        for (var studentEntry : scoresByStudent.entrySet()) {

            List<AssessmentScore> studentScores = studentEntry.getValue();

            // Group subjects for the student
            Map<String, List<AssessmentScore>> subjectMap =
                    studentScores.stream().collect(Collectors.groupingBy(
                            s -> s.getStudentAssessment()
                                    .getTeacherSubject()
                                    .getSubject()
                                    .getId()
                    ));

            for (var subjectEntry : subjectMap.entrySet()) {

                List<AssessmentScore> subjectScores = subjectEntry.getValue();

                // Sort assessments (Exam → Test 2 → Test 1)
                subjectScores.sort((a, b) ->
                        Integer.compare(
                                b.getCycle().getAssessment().getPosition(),
                                a.getCycle().getAssessment().getPosition()
                        )
                );

                double previousAverage = 0;

                for (AssessmentScore score : subjectScores) {

                    if (!score.isCompleted()) continue;


                    double currentAverage = score.getWeightedScore();

                    String trend = "NEUTRAL";

                    if (previousAverage == 0) {
                        trend = "IMPROVED";
                    } else if (currentAverage > previousAverage) {
                        trend = "IMPROVED";
                    } else if (currentAverage < previousAverage) {
                        trend = "DROPPED";
                    }

                    previousAverage = currentAverage;

                    var scoreGrade =
                            resolveScoreGradeUseCase.execute(
                                    request.schoolId(),
                                    score.getScore(),
                                    score.getStatus()
                            );

                    AssessmentScoreDTO dto =
                            AssessmentScoreMapper.toDTO(
                                    score,
                                    scoreGrade,
                                    trend,
                                    currentAverage
                            );

                    leaderboard.add(dto);
                }
            }
        }

        // Manual pagination
        if (size <= 0) {
            return new PageImpl<>(leaderboard, pageable, leaderboard.size());
        }

        int start = Math.min((page - 1) * size, leaderboard.size());
        int end = Math.min(start + size, leaderboard.size());

        List<AssessmentScoreDTO> pageContent =
                leaderboard.subList(start, end);

        return new PageImpl<>(pageContent, pageable, leaderboard.size());
    }
}