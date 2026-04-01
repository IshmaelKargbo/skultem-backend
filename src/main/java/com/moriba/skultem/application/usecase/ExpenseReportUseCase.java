package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExpenseDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.mapper.ExpenseMapper;
import com.moriba.skultem.domain.model.Expense;
import com.moriba.skultem.domain.repository.ExpenseRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseReportUseCase {

    private final ExpenseRepository repo;

    public Page<ExpenseDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

        List<Filter> filters = request.filters();

        Page<Expense> res = repo.runReport(
                request.schoolId(),
                filters,
                pageable
        );

        return res.map(e -> ExpenseMapper.toDTO(e));
    }
}