package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AttendanceMapper;
import com.moriba.skultem.domain.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAttendanceUseCase {
    private final AttendanceRepository repo;
    private final com.moriba.skultem.application.services.SectionScopeService sectionScopeService;

    public AttendanceDTO execute(String schoolId, String id) {
        // A record from a class outside the caller's sections is treated as not there.
        var attendance = repo.findByIdAndSchoolId(id, schoolId)
                .filter(a -> a.getEnrollment() == null || a.getEnrollment().getClazz() == null
                        || sectionScopeService.levels().contains(a.getEnrollment().getClazz().getLevel()))
                .orElseThrow(() -> new NotFoundException("Attendance not found"));
        return AttendanceMapper.toDTO(attendance);
    }
}
