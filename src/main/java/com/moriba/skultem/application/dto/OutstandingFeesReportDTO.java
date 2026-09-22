package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record OutstandingFeesReportDTO(
        BigDecimal totalOutstanding,
        int studentsWithOutstanding,
        List<ClassOutstandingRowDTO> byClass,
        List<FeeTypeOutstandingRowDTO> byFeeType,
        List<StudentFeeBalanceDTO> topStudents) {

    public record ClassOutstandingRowDTO(
            String classSessionId,
            String className,
            BigDecimal outstanding,
            int studentsWithOutstanding) {
    }

    public record FeeTypeOutstandingRowDTO(
            String feeCategoryId,
            String feeCategoryName,
            BigDecimal outstanding,
            int studentsWithOutstanding) {
    }
}
