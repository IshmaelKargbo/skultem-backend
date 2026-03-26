package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
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
public class GradeReportUseCase {

    private final AssessmentScoreRepository repo;
    private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

    public Page<AssessmentScoreDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

        List<Filter> filters = request.filters();

        Page<AssessmentScore> res = repo.runReport(
                request.schoolId(),
                filters,
                pageable);

        return res.map(e -> {
            var score = resolveScoreGradeUseCase.execute(request.schoolId(), e.getScore(), e.getStatus());
            return AssessmentScoreMapper.toDTO(e, score);
        });
    }
}