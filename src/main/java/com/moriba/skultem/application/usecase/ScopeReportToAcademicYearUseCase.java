package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;

import lombok.RequiredArgsConstructor;

/**
 * Scopes a report/widget query (the shared filter-driven engine behind {@code ReportExportService}
 * and {@code WidgetUsecase}) to one academic year - for the handful of entities where "which year"
 * is even a meaningful question: transactions, payments, fees. Every other entity is left exactly
 * as the caller built it, and a request that already brings its own academic-year filter is never
 * second-guessed - an explicit choice always wins over this default.
 */
@Service
@RequiredArgsConstructor
public class ScopeReportToAcademicYearUseCase {

    private static final Map<String, String> ACADEMIC_YEAR_FIELD_BY_ENTITY = Map.of(
            "transactions", "academicYear.id",
            "payments", "fee.academicYear.id",
            "fees", "fee.academicYear.id",
            "ledger", "academicYearId");

    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public List<Filter> execute(String schoolId, String entity, List<Filter> filters, String academicYearId) {
        String field = ACADEMIC_YEAR_FIELD_BY_ENTITY.get(entity == null ? "" : entity.toLowerCase());
        if (field == null) {
            return filters;
        }

        boolean alreadyScoped = filters != null && filters.stream().anyMatch(f -> field.equalsIgnoreCase(f.field()));
        if (alreadyScoped) {
            return filters;
        }

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        List<Filter> augmented = new ArrayList<>(filters != null ? filters : List.of());
        augmented.add(new Filter(field, FilterOperator.EQUALS, "select", academicYear.getId(), null, null));
        return augmented;
    }
}
