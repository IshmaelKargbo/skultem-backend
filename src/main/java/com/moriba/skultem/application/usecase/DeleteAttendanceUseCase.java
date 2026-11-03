package com.moriba.skultem.application.usecase;

import org.springframework.cache.annotation.CacheEvict;
import com.moriba.skultem.config.CacheConfig;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AttendanceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Changes what ComputeClassAttentionUseCase reports - clears its cache once a call here succeeds.
@CacheEvict(cacheNames = CacheConfig.CLASS_ATTENTION, allEntries = true)
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteAttendanceUseCase {
    private final AttendanceRepository repo;
    private final com.moriba.skultem.application.services.SectionScopeService sectionScopeService;

    public void execute(String schoolId, String id) {
        // A record from a class outside the caller's sections is treated as not there.
        var attendance = repo.findByIdAndSchoolId(id, schoolId)
                .filter(a -> a.getEnrollment() == null || a.getEnrollment().getClazz() == null
                        || sectionScopeService.levels().contains(a.getEnrollment().getClazz().getLevel()))
                .orElseThrow(() -> new NotFoundException("Attendance not found"));
        repo.delete(attendance);
    }
}
