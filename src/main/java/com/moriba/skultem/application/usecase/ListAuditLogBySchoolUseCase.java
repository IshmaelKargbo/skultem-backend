package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.repository.AuditLogRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAuditLogBySchoolUseCase {

    private final AuditLogRepository repo;

    public Page<AuditLog> execute(String schoolId, String academicYearId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllBySchoolIdAndAcademicYear(schoolId, academicYearId, pageable);
    }
}
