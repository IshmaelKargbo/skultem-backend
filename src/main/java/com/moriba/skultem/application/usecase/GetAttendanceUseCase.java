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

    public AttendanceDTO execute(String schoolId, String id) {
        var attendance = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Attendance not found"));
        return AttendanceMapper.toDTO(attendance);
    }
}
