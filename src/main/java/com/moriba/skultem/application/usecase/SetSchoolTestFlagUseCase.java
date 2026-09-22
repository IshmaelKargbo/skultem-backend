package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** System-admin-only: marks/unmarks a school as a test school - see WipeTestSchoolDataUseCase. */
@Service
@Transactional
@RequiredArgsConstructor
public class SetSchoolTestFlagUseCase {
    private final SchoolRepository repo;

    @AuditLogAnnotation(action = "SCHOOL_TEST_FLAG_CHANGED")
    public SchoolDTO execute(String schoolId, boolean testSchool) {
        var school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        school.markAsTest(testSchool);
        repo.save(school);
        return SchoolMapper.toDTO(school);
    }
}
