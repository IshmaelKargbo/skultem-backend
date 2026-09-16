package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionAttendanceDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The Daily Register is the same roster+rollup GetClassSessionAttendanceUseCase already builds
// for the Mark Attendance sheet (now enriched with recordedBy/recordedAt) - this use case exists
// as its own entry point only so the /attendance/register endpoint can carry its own
// teacher-owns-this-session @PreAuthorize check, without touching the existing
// /attendance/session/{id} route's behavior.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateDailyAttendanceRegisterUseCase {

    private final GetClassSessionAttendanceUseCase getClassSessionAttendanceUseCase;

    public ClassSessionAttendanceDTO execute(String schoolId, String classSessionId, LocalDate date) {
        return getClassSessionAttendanceUseCase.execute(schoolId, classSessionId, date);
    }
}
