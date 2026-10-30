package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.LibraryAttendanceCorrection;

public interface LibraryAttendanceCorrectionRepository {

    void save(LibraryAttendanceCorrection domain);

    List<LibraryAttendanceCorrection> findBySession(String sessionId, String schoolId);
}
