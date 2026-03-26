package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ActiveCycleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ActiveCycleUseCase {

    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository classSessionRepo;

    public ActiveCycleDTO execute(String schoolId, String sessionId) {
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        String className = "";

        if (!sessionId.equals("all") && !sessionId.isBlank()) {
            var session = classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class session not found"));
            academicYear = session.getAcademicYear();
            className = session.getClazz().getName();
        }

        var terms = termRepository.findAllBySchoolIdAndAcademicYear(schoolId, academicYear.getId(), Pageable.unpaged())
                .getContent();
        return new ActiveCycleDTO(academicYear.getId(), academicYear.getName(), className, terms);
    }
}
