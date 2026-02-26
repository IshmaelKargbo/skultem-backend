package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceMapper;
import com.moriba.skultem.domain.repository.AttendanceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateAttendanceUseCase {
    private final AttendanceRepository repo;

    public AttendanceDTO execute(
            String schoolId,
            String id,
            LocalDate date,
            boolean present,
            boolean excused,
            boolean late,
            String reason,
            boolean holiday) {
        var attendance = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Attendance not found"));

        if (!attendance.getDate().equals(date) && repo.existsByEnrollmentAndDateAndSchoolId(
                attendance.getEnrollment().getId(), date, schoolId)) {
            throw new AlreadyExistsException("Attendance already exists for this enrollment and date");
        }

        if (present && excused) {
            throw new RuleException("Present attendance cannot be marked as excused");
        }

        if (holiday && present) {
            throw new RuleException("Holiday attendance cannot be marked as present");
        }

        if (!present && !excused && (reason == null || reason.isBlank())) {
            throw new RuleException("Reason is required for an unexcused absence");
        }

        attendance.update(present, excused, late, reason, holiday);
        repo.save(attendance);
        return AttendanceMapper.toDTO(attendance);
    }
}
