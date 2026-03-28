package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.application.dto.StudentAssessmentDTO;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.StudentAssessment;

public class StudentAssessmentMapper {

        public static StudentAssessmentDTO toDTO(
                        StudentAssessment param,
                        List<AssessmentScore> assessments) {

                String name = param.getEnrollment().getStudent().getName();

                List<AssessmentScoreDTO> scores = assessments.stream()
                                .map(score -> AssessmentScoreMapper.toDTO(score))
                                .toList();

                return new StudentAssessmentDTO(
                                param.getId(),
                                name,
                                scores,
                                param.getCreatedAt(),
                                param.getUpdatedAt());
        }
}