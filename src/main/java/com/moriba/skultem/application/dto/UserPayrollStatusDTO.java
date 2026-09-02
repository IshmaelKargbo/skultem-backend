package com.moriba.skultem.application.dto;

// Whether a User account already has a staff/payroll record (a Teacher row - see
// CreateUserUseCase.addToPayroll/IncludeUserInPayrollUseCase) backing it. teacherId is null unless
// onPayroll is true.
public record UserPayrollStatusDTO(boolean onPayroll, String teacherId, String staffId, String designation,
                boolean teaching) {
}
