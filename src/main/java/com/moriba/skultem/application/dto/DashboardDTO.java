package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

public record DashboardDTO(long totalStudents, long studentGrowth, long totalTeachers, String activeYear,
                String activeTerm, BigDecimal monthlyRevenue, BigDecimal revenueGrowthPercent) {

}