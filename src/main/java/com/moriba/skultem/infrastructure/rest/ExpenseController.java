package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ExpenseCategoryDTO;
import com.moriba.skultem.application.dto.ExpenseDTO;
import com.moriba.skultem.application.usecase.CreateExpenseCategoryUseCase;
import com.moriba.skultem.application.usecase.CreateExpenseUseCase;
import com.moriba.skultem.application.usecase.ListExpenseBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListExpenseCategoryBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateExpenseCategoryDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateExpenseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/expense")
@RequiredArgsConstructor
public class ExpenseController {

        private final CreateExpenseCategoryUseCase createExpenseCategoryUseCase;
        private final ListExpenseBySchoolUseCase listExpenseBySchoolUseCase;
        private final ListExpenseCategoryBySchoolUseCase listExpenseCategoryBySchoolUseCase;
        private final CreateExpenseUseCase createExpenseUseCase;

        @PostMapping("/category")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ACCOUNTANT')")
        public ApiResponse<Object> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateExpenseCategoryDTO param) {
                var res = createExpenseCategoryUseCase.execute(school, param.name(), param.description());
                return new ApiResponse<>("success", 200, "Expense category created successfully", res);
        }

        @PostMapping()
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ACCOUNTANT')")
        public ApiResponse<ExpenseDTO> createExpense(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateExpenseDTO param) {
                var res = createExpenseUseCase.execute(school, param.name(), param.description(), param.category(), param.amount(), param.expenseDate());
                return new ApiResponse<>("success", 200, "Expense created successfully", res);
        }

        @GetMapping()
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<List<ExpenseDTO>> listExpense(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listExpenseBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Expenses fetched successfully",
                                list,
                                meta);
        }

        @GetMapping("/category")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'PROPRIETOR', 'ACCOUNTANT')")
        public ApiResponse<List<ExpenseCategoryDTO>> listCategory(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listExpenseCategoryBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Expense categories fetch successfully", list,
                                meta);
        }
}
