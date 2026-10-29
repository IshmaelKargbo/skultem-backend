package com.moriba.skultem.application.dto;

public record LibrarySettingDTO(int maxStudentLoans, int maxTeacherLoans, boolean blockWhenOverdue,
        int defaultLoanDays) {
}
