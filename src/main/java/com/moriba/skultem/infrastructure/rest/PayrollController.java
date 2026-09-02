package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.moriba.skultem.application.dto.PayrollRunDTO;
import com.moriba.skultem.application.dto.PayrollRunDetailDTO;
import com.moriba.skultem.application.dto.PayrollSummaryDTO;
import com.moriba.skultem.application.dto.PayslipDTO;
import com.moriba.skultem.application.dto.SalaryStructureDTO;
import com.moriba.skultem.application.services.PayrollService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreatePayrollRunDTO;
import com.moriba.skultem.infrastructure.rest.dto.SetSalaryStructureDTO;
import com.moriba.skultem.infrastructure.rest.dto.TogglePayslipDTO;
import com.moriba.skultem.infrastructure.rest.mapper.MetaMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/salary/summary")
    public ApiResponse<PayrollSummaryDTO> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Payroll summary fetched successfully", payrollService.summary(school));
    }

    @PostMapping("/salary")
    public ApiResponse<SalaryStructureDTO> setSalary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SetSalaryStructureDTO param) {
        var res = payrollService.setSalary(school, param.teacherId(), param.basicSalary(), param.allowances(),
                param.deductions());
        return new ApiResponse<>("success", 200, "Salary structure saved successfully", res);
    }

    @GetMapping("/salary")
    public ApiResponse<List<SalaryStructureDTO>> listSalaries(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {
        var res = payrollService.listSalaries(school, page, size, search);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Salary structures fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/salary/teacher/{teacherId}")
    public ApiResponse<SalaryStructureDTO> getSalaryByTeacher(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherId) {
        return new ApiResponse<>("success", 200, "Salary structure fetched successfully",
                payrollService.getSalaryByTeacher(school, teacherId));
    }

    @GetMapping("/salary/teacher/{teacherId}/history")
    public ApiResponse<List<PayslipDTO>> getSalaryHistory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherId) {
        return new ApiResponse<>("success", 200, "Payslip history fetched successfully",
                payrollService.getSalaryHistory(school, teacherId));
    }

    @PostMapping("/run")
    public ApiResponse<PayrollRunDTO> createRun(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreatePayrollRunDTO param) {
        return new ApiResponse<>("success", 200, "Payroll run created successfully",
                payrollService.createRun(school, param.period(), param.payDate()));
    }

    @GetMapping("/run")
    public ApiResponse<List<PayrollRunDTO>> listRuns(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = payrollService.listRuns(school, page, size);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Payroll runs fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/run/{runId}")
    public ApiResponse<PayrollRunDetailDTO> getRun(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String runId) {
        return new ApiResponse<>("success", 200, "Payroll run fetched successfully",
                payrollService.getRunDetail(school, runId));
    }

    @PatchMapping("/run/{runId}/item/{payslipId}")
    public ApiResponse<PayslipDTO> setIncluded(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String runId,
            @PathVariable String payslipId,
            @Valid @RequestBody TogglePayslipDTO param) {
        return new ApiResponse<>("success", 200, "Payslip updated successfully",
                payrollService.setIncluded(school, runId, payslipId, param.included()));
    }

    @PatchMapping("/run/{runId}/generate")
    public ApiResponse<PayrollRunDTO> generate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String runId) {
        return new ApiResponse<>("success", 200, "Payroll run generated successfully",
                payrollService.generateRun(school, runId));
    }

    @PatchMapping("/run/{runId}/publish")
    public ApiResponse<PayrollRunDTO> publish(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String runId) {
        return new ApiResponse<>("success", 200, "Payslips published successfully",
                payrollService.publishRun(school, runId));
    }

    @GetMapping("/run/{runId}/payslip/{teacherId}")
    public ApiResponse<PayslipDTO> getPayslip(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String runId,
            @PathVariable String teacherId) {
        return new ApiResponse<>("success", 200, "Payslip fetched successfully",
                payrollService.getPayslip(school, runId, teacherId));
    }
}
