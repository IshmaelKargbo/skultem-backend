package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

public class ReportCardMapper {
    public static ReportCardSummaryDTO toSummaryDTO(ReportCard param) {
        return new ReportCardSummaryDTO(param.getId(), param.getStudentId(), param.getStudentName(),
                param.getAdmissionNumber(), param.getPhoto(), param.getClassName(), param.getTermName(),
                param.getAcademicYearName(), param.getAverage(), param.getPosition(), param.getOverallGrade(),
                param.isPassed(), param.getDownloadCount(), param.getGeneratedAt());
    }

    public static ReportCardDTO toDTO(ReportCard param, SchoolDTO school, ReportCardSettingDTO settings) {
        var subjects = JsonMapper.fromJsonList(param.getSubjects(), ReportCardSubjectDTO.class);

        return new ReportCardDTO(param.getId(), param.getStudentId(), param.getStudentName(),
                param.getAdmissionNumber(), param.getPhoto(), param.getClassId(), param.getClassName(),
                param.getClassSize(), param.getTermId(), param.getTermName(), param.getAcademicYearName(),
                param.getAverage(), param.getPosition(), param.getOverallGrade(), param.isPassed(),
                param.getAttendancePercentage(), param.getRemark(), subjects, param.getGeneratedAt(),
                param.getDownloadCount(), school, settings);
    }
}
