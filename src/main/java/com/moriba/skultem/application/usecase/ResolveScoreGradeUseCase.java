package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ResolveScoreGradeUseCase {

    private final SchoolRepository schoolRepository;

    public String execute(String schoolId, int score) {
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));
        return school.resolveGrade(score);
    }
}
