package com.moriba.skultem.infrastructure.rest;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.dto.FeeDiscountDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.dto.StudentLedgerPagedDTO;
import com.moriba.skultem.application.usecase.AssignFeeToStudentUseCase;
import com.moriba.skultem.application.usecase.AssignFeeToStudentUseCase.AssignFeeToStudentRecord;
import com.moriba.skultem.application.usecase.CountStudentByFeeUseCase;
import com.moriba.skultem.application.usecase.CreateFeeCategoryUseCase;
import com.moriba.skultem.application.usecase.CreateFeeDiscountUseCase;
import com.moriba.skultem.application.usecase.CreateFeeDiscountUseCase.DiscountRecord;
import com.moriba.skultem.application.usecase.CreateFeeStructureUseCase;
import com.moriba.skultem.application.usecase.CreateFeeStructureUseCase.StructureRecord;
import com.moriba.skultem.application.usecase.ListFeeCategoryBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListFeeStructureBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStudentLedgerBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssignFeeToStudentDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateFeeCategoryDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateFeeDiscountDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateFeeStructureDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moriba.skultem.application.dto.FeeDiscountReportDTO;
import com.moriba.skultem.application.dto.StudentLedgerReportDTO;
import com.moriba.skultem.application.usecase.CountStudentFeesUseCase;
import com.moriba.skultem.application.usecase.FeeDiscountReportUseCase;
import com.moriba.skultem.application.usecase.ListFeeDiscountBySchoolUseCase;
import com.moriba.skultem.application.usecase.StudentLedgerReportUseCase;
import com.moriba.skultem.domain.model.FeeDiscount.Kind;

@RestController
@RequestMapping("/api/v1/fee")
@RequiredArgsConstructor
public class FeeController {

        private final ListFeeCategoryBySchoolUseCase feeCategoryBySchoolUseCase;
        private final CreateFeeCategoryUseCase createFeeCategoryUseCase;
        private final ListFeeStructureBySchoolUseCase listFeeStructureBySchoolUseCase;
        private final ListStudentLedgerBySchoolUseCase listStudentLedgerBySchoolUseCase;
        private final ListFeeDiscountBySchoolUseCase listFeeDiscountBySchoolUseCase;
        private final CreateFeeStructureUseCase createFeeStructureUseCase;
        private final FeeDiscountReportUseCase feeDiscountReportUseCase;
        private final CountStudentFeesUseCase countStudentFeesUseCase;
        private final CreateFeeDiscountUseCase createFeeDiscountUseCase;
        private final CountStudentByFeeUseCase countStudentByFeeUseCase;
        private final StudentLedgerReportUseCase studentLedgerReportUseCase;
        private final AssignFeeToStudentUseCase assignFeeToStudentUseCase;

        @PostMapping("/category")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<Object> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateFeeCategoryDTO param) {
                var res = createFeeCategoryUseCase.execute(school, param.name(), param.description());
                return new ApiResponse<>("success", 200, "Fee category created successfully", res);
        }

        @PostMapping("/structure")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<FeeStructureDTO> createStructure(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateFeeStructureDTO param) {
                var payload = new StructureRecord(school, param.classId(), param.feeCategory(), param.termId(),
                                param.amount(),
                                param.dueDate(), param.allowInstallment(), param.description());
                var res = createFeeStructureUseCase.execute(payload);
                return new ApiResponse<>("success", 200, "Fee structure created successfully", res);
        }

        @PostMapping("/structure/assign")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<StudentFeeDTO> assignStructureToStudent(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody AssignFeeToStudentDTO param) {
                var payload = new AssignFeeToStudentRecord(school, param.studentId(), param.feeId());
                var res = assignFeeToStudentUseCase.execute(payload);
                return new ApiResponse<>("success", 200, "Fee assigned to student successfully", res);
        }

        @GetMapping("/structure")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<List<FeeStructureDTO>> listStructure(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listFeeStructureBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Fee structures fetched successfully",
                                list,
                                meta);
        }

        @GetMapping("/structure/count/{feeId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<Long> countFees(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String feeId) {

                var res = countStudentByFeeUseCase.execute(school, feeId);
                return new ApiResponse<>("success", 200, "Count student by fee structure fetched successfully",
                                res);
        }

        @GetMapping("/student/{studentId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<BigDecimal> countStudentFees(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String studentId) {

                var res = countStudentFeesUseCase.execute(school, studentId);
                return new ApiResponse<>("success", 200, "Count student by fee structure fetched successfully",
                                res);
        }

        @PostMapping("/discount")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<Object> applyDiscount(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateFeeDiscountDTO param) {

                var kind = Kind.valueOf(param.kind());
                var payload = new DiscountRecord(school, kind, param.name(), param.value(), param.expiryDate(),
                                param.studentId(), param.feeId(), param.reason());
                createFeeDiscountUseCase.execute(payload);
                return new ApiResponse<>("success", 200, "create fee discount successfully", null);
        }

        @GetMapping("/discount")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<List<FeeDiscountDTO>> listAllDiscount(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listFeeDiscountBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Fee discounts fetch successfully", list,
                                meta);
        }

        @GetMapping("/discount/report")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<FeeDiscountReportDTO> calculateDiscountReport(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school) {

                var res = feeDiscountReportUseCase.calculateReport(school);

                return new ApiResponse<>("success", 200, "Fee discount report fetch successfully", res);
        }

        @GetMapping("/ledger")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<StudentLedgerPagedDTO> applyDiscount(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listStudentLedgerBySchoolUseCase.execute(school, page - 1, size);
                Map<String, Object> meta = Map.of(
                                "page", res.page() + 1,
                                "size", res.size(),
                                "count", res.totalElements(),
                                "pages", res.totalPages());

                return new ApiResponse<>("success", 200, "Student ledger fetch successfully",
                                res,
                                meta);
        }

        @GetMapping("/ledger/report")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<StudentLedgerReportDTO> calculateLedgerReport(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school) {

                var res = studentLedgerReportUseCase.calculateReport(school);
                return new ApiResponse<>("success", 200, "Student ledger report successfully",
                                res);
        }

        @GetMapping("/category")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<List<FeeCategoryDTO>> list(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = feeCategoryBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Fee category fetched successfully", list,
                                meta);
        }
}
