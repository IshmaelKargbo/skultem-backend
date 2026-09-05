package com.moriba.skultem.application.dto;

// One flagged student in ComputeClassAttentionUseCase's result - attendanceRate/academicAverage are
// null when there isn't enough data yet (no attendance marked in the window, or no scores recorded
// this term) rather than 0, so the UI can tell "no data" apart from "actually zero".
public record StudentAttentionDTO(
        String studentId,
        String enrollmentId,
        String givenNames,
        String familyName,
        String photo,
        Double attendanceRate,
        Double academicAverage,
        boolean attendanceFlag,
        boolean academicFlag) {
}
