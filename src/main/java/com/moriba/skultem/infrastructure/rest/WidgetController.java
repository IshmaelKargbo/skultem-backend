package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionScoped;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.Widget;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.application.usecase.WidgetUsecase;
import com.moriba.skultem.config.CacheConfig;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Metric;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.WidgetDTO;
import com.moriba.skultem.infrastructure.security.AuthUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/widget")
@RequiredArgsConstructor
public class WidgetController {

        private final WidgetUsecase widgetUsecase;
        private final CacheManager cacheManager;

        @SectionScoped
        @PostMapping("/run")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
        public ApiResponse<Object> runReport(
                        @AuthenticationPrincipal AuthUser user,
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestHeader(value = SectionScopeService.VIEW_HEADER, required = false) String viewSection,
                        @RequestParam int page,
                        @RequestParam int size,
                        @RequestParam(required = false) String academicYearId,
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

                // Dashboards ask for the same widgets on every visit - answered from a 30-second cache
                // (CacheConfig.WIDGETS). Per user, role and section view, since those shape what a widget may show.
                Cache cache = cacheManager.getCache(CacheConfig.WIDGETS);
                String key = String.join("|", school, user.userId(), String.valueOf(user.activeRole()),
                                String.valueOf(viewSection), String.valueOf(academicYearId),
                                String.valueOf(page), String.valueOf(size), widget.toString());
                Object res = cache != null ? cache.get(key, Object.class) : null;
                if (res == null) {
                        res = widgetUsecase.runAnalytics(school, widget, page, size, academicYearId);
                        // Only successful results are kept - a failure throws before this and is retried next time.
                        if (cache != null && res != null)
                                cache.put(key, res);
                }

                return new ApiResponse<>("success", 200, "Widget fetch successfully", res);
        }
}
