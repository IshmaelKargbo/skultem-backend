package com.moriba.skultem.application.usecase;

import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.GradeBandDTO;
import com.moriba.skultem.application.dto.GradingScaleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.GradeBand;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetSchoolGradingScaleUseCase {

    private final SchoolRepository schoolRepository;

    public GradingScaleDTO execute(String schoolId) {
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));

        var bands = school.getGradingScale().stream()
                .sorted(Comparator.comparingInt((GradeBand b) -> b.maxScore()).reversed())
                .map(b -> new GradeBandDTO(b.minScore(), b.maxScore(), b.grade()))
                .toList();

        return new GradingScaleDTO(bands);
    }
}
