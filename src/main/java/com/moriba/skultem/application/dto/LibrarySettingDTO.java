package com.moriba.skultem.application.dto;

// sectionId: whose rules these are (null = the school's). ownRules is false when a section has none of its own yet and
// is using the school's.
public record LibrarySettingDTO(int maxStudentLoans, int maxTeacherLoans, boolean blockWhenOverdue,
        int defaultLoanDays, String sectionId, boolean ownRules) {
}
