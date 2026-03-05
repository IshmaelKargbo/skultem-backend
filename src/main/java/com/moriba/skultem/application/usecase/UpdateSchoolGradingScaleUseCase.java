package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.GradeBandDTO;
import com.moriba.skultem.application.dto.GradingScaleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.vo.GradeBand;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSchoolGradingScaleUseCase {

    private final SchoolRepository schoolRepository;

    public GradingScaleDTO execute(String schoolId, List<GradeBandDTO> bands) {
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));

        var gradingScale = bands.stream()
                .map(band -> new GradeBand(band.minScore(), band.maxScore(), band.grade().trim()))
                .toList();

        school.setGradingScale(gradingScale);
        schoolRepository.save(school);

        var response = school.getGradingScale().stream()
                .sorted(Comparator.comparingInt((GradeBand b) -> b.maxScore()).reversed())
                .map(b -> new GradeBandDTO(b.minScore(), b.maxScore(), b.grade()))
                .toList();

        return new GradingScaleDTO(response);
    }
}
