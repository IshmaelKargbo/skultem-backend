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
import com.moriba.skultem.application.dto.SalaryTemplateDTO;
import com.moriba.skultem.application.services.PayrollService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreatePayrollRunDTO;
import com.moriba.skultem.infrastructure.rest.dto.SaveSalaryTemplateDTO;
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

    // Self-service - overrides the class-level admin-only restriction, same pattern as the
    // attendance/curriculum "me" endpoints. Scoped to the signed-in user's own teacher record
    // (see PayrollService#getMySalaryHistory / #getMyPayslip) rather than an admin-supplied
    // teacherId, so a teacher can't pull another staff member's payroll data.
    @GetMapping("/me/history")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<PayslipDTO>> myHistory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId) {
        return new ApiResponse<>("success", 200, "Payslip history fetched successfully",
                payrollService.getMySalaryHistory(school, userId));
    }

    @GetMapping("/me/payslip/{runId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<PayslipDTO> myPayslip(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String runId) {
        return new ApiResponse<>("success", 200, "Payslip fetched successfully",
                payrollService.getMyPayslip(school, userId, runId));
    }

    @GetMapping("/salary/summary")
    public ApiResponse<PayrollSummaryDTO> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Payroll summary fetched successfully", payrollService.summary(school));
    }

    @PostMapping("/salary")
    public ApiResponse<SalaryStructureDTO> setSalary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SetSalaryStructureDTO param) {
        var res = payrollService.setSalary(school, param.teacherId(), param.templateId(), param.basicSalary(),
                param.allowances(), param.deductions());
        return new ApiResponse<>("success", 200, "Salary structure saved successfully", res);
    }

    @GetMapping("/salary")
    public ApiResponse<List<SalaryStructureDTO>> listSalaries(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        var res = payrollService.listSalaries(school, page, size, search, sortBy, direction);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Salary structures fetched successfully", res.getContent(), meta);
    }

    @PostMapping("/salary-template")
    public ApiResponse<SalaryTemplateDTO> createSalaryTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveSalaryTemplateDTO param) {
        var res = payrollService.createSalaryTemplate(school, param.name(), param.basicSalary(), param.allowances(),
                param.deductions());
        return new ApiResponse<>("success", 200, "Salary template created successfully", res);
    }

    @PutMapping("/salary-template/{templateId}")
    public ApiResponse<SalaryTemplateDTO> updateSalaryTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String templateId,
            @Valid @RequestBody SaveSalaryTemplateDTO param) {
        var res = payrollService.updateSalaryTemplate(school, templateId, param.name(), param.basicSalary(),
                param.allowances(), param.deductions());
        return new ApiResponse<>("success", 200, "Salary template updated successfully", res);
    }

    @GetMapping("/salary-template")
    public ApiResponse<List<SalaryTemplateDTO>> listSalaryTemplates(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        var res = payrollService.listSalaryTemplates(school, page, size, search, sortBy, direction);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Salary templates fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/salary-template/{templateId}")
    public ApiResponse<SalaryTemplateDTO> getSalaryTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String templateId) {
        return new ApiResponse<>("success", 200, "Salary template fetched successfully",
                payrollService.getSalaryTemplate(school, templateId));
    }

    @DeleteMapping("/salary-template/{templateId}")
    public ApiResponse<Void> deleteSalaryTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String templateId) {
        payrollService.deleteSalaryTemplate(school, templateId);
        return new ApiResponse<>("success", 200, "Salary template deleted successfully", null);
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
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        var res = payrollService.listRuns(school, page, size, search, status, sortBy, direction);
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
