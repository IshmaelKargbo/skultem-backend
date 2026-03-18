package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Metric;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.WidgetDTO;
import com.moriba.skultem.infrastructure.widget.WidgetService;
import com.moriba.skultem.infrastructure.widget.dto.Widget;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/widget")
@RequiredArgsConstructor
public class WidgetController {

        private final WidgetService widgetService;

        @PostMapping("/run")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
        public ApiResponse<Object> runReport(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam int page,
                        @RequestParam int size,
                        @RequestBody WidgetDTO param) {

                List<Filter> filters = param.filters() != null
                                ? param.filters().stream()
                                                .map(e -> new Filter(e.field(), e.operator(), e.type(), e.value(),
                                                                e.valueTo(), e.values()))
                                                .toList()
                                : List.of();

                List<Metric> metrics = param.metrics() != null
                                ? param.metrics().stream()
                                                .map(e -> {
                                                        List<Filter> filteredRecords = e.filters() != null
                                                                        ? e.filters().stream()
                                                                                        .map(i -> new Filter(i.field(),
                                                                                                        i.operator(),
                                                                                                        i.type(),
                                                                                                        i.value(),
                                                                                                        i.valueTo(),
                                                                                                        i.values()))
                                                                                        .toList()
                                                                        : List.of();

                                                        return new Metric(e.name(), e.aggregation(), e.field(),
                                                                        e.tags(), e.value(), filteredRecords);
                                                })
                                                .toList()
                                : List.of();

                var widget = new Widget(param.entity(), filters, metrics, param.chartType(), param.title());
                var res = widgetService.runAnalytics(school, widget, page, size);

                return new ApiResponse<>("success", 200, "Widget fetch successfully", res);
        }
}
