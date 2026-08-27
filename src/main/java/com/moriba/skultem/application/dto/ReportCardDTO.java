package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record ReportCardDTO(String id, String studentId, String studentName, String admissionNumber, String photo,
        String classId, String className, int classSize, String termId, String termName, String academicYearName,
        double average, int position, String overallGrade, boolean passed, Double attendancePercentage,
        String remark, List<ReportCardSubjectDTO> subjects, Instant generatedAt, int downloadCount,
        SchoolDTO school, ReportCardSettingDTO settings) {
}
