package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.infrastructure.persistence.entity.ReportCardEntity;

public class ReportCardMapper {
    public static ReportCard toDomain(ReportCardEntity param) {
        if (param == null) return null;

        return new ReportCard(param.getId(), param.getSchoolId(), param.getStudentId(), param.getStudentName(),
                param.getAdmissionNumber(), param.getPhoto(), param.getClassId(), param.getClassName(),
                param.getClassSize(), param.getTermId(), param.getTermName(), param.getAcademicYearName(),
                param.getAverage(), param.getPosition(), param.getOverallGrade(), param.isPassed(),
                param.getAttendancePercentage(), param.getRemark(), param.getSubjects(), param.getGeneratedBy(),
                param.getGeneratedAt(), param.getDownloadCount(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ReportCardEntity toEntity(ReportCard param) {
        if (param == null) return null;

        return ReportCardEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .studentId(param.getStudentId())
                .studentName(param.getStudentName())
                .admissionNumber(param.getAdmissionNumber())
                .photo(param.getPhoto())
                .classId(param.getClassId())
                .className(param.getClassName())
                .classSize(param.getClassSize())
                .termId(param.getTermId())
                .termName(param.getTermName())
                .academicYearName(param.getAcademicYearName())
                .average(param.getAverage())
                .position(param.getPosition())
                .overallGrade(param.getOverallGrade())
                .passed(param.isPassed())
                .attendancePercentage(param.getAttendancePercentage())
                .remark(param.getRemark())
                .subjects(param.getSubjects())
                .generatedBy(param.getGeneratedBy())
                .generatedAt(param.getGeneratedAt())
                .downloadCount(param.getDownloadCount())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
