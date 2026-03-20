package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ActiveCycleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ActiveCycleUseCase {

    private final TermRepository termRepository;
    private final ClassSessionRepository classSessionRepo;

    public ActiveCycleDTO execute(String schoolId, String sessionId) {
        var session = classSessionRepo.findByIdAndSchoolId(sessionId, schoolId).orElseThrow(() -> new NotFoundException("Class session not found"));
        var academicYear = session.getAcademicYear();

        var terms = termRepository.findAllBySchoolIdAndAcademicYear(schoolId, academicYear.getId(), Pageable.unpaged()).getContent();
        return new ActiveCycleDTO(academicYear.getId(), academicYear.getName(), session.getClazz().getName(), terms);
    }
}
