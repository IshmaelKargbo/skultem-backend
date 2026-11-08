package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.vo.Level;

public record ReportCardDTO(String id, String studentId, String studentName, String admissionNumber, String photo,
        String classId, String className, int classSize, String termId, String termName, String academicYearName,
        double average, int position, String overallGrade, boolean passed, Double attendancePercentage,
        String remark, List<ReportCardSubjectDTO> subjects, Instant generatedAt, int downloadCount,
        SchoolDTO school, ReportCardSettingDTO settings, Level level, String scopeLabel, String gradeRemark,
        String reviewStatus, String reviewNote, String reviewedByName, Instant reviewedAt,
        // The class teacher who signed it on approval, and their signature image (null = blank line).
        String classTeacherName, String classTeacherSignature) {
}
