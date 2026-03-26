package com.moriba.skultem.application.usecase;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ChartData;
import com.moriba.skultem.application.dto.Dataset;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.TableData;
import com.moriba.skultem.application.dto.Widget;
import com.moriba.skultem.application.dto.WidgetResponse;
import com.moriba.skultem.domain.vo.Metric;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WidgetUsecase {

    private static final Logger log = LoggerFactory.getLogger(WidgetUsecase.class);

    private final AttendanceReportUseCase attendanceReportUseCase;
    private final StudentReportUseCase studentReportUseCase;
    private final FeeReportUseCase feeReportUseCase;
    private final GradeReportUseCase gradeReportUseCase;
    private final PaymentReportUseCase paymentReportUseCase;
    private final ClassReportUseCase classReportUseCase;
    private final ParentReportUseCase parentReportUseCase;
    private final TeacherReportUseCase teacherReportUseCase;

    public WidgetResponse<?> runAnalytics(String schoolId, Widget request, int page, int size) {
        if (request == null || request.metrics() == null || request.metrics().isEmpty()) {
            throw new IllegalArgumentException("Invalid widget request");
        }

        List<?> records = loadRecords(schoolId, request, page, size);

        if (records.isEmpty()) {
            // return empty chart or table
            Object emptyData = request.chartType().equalsIgnoreCase("table")
                    ? emptyTable(request)
                    : emptyChart(request);
            return new WidgetResponse<>(200, "Widget fetch successfully", "success", emptyData);
        }

        String groupField = firstTag(request, "groupBy");
        String interval = firstTag(request, "interval", "day");

        Map<String, List<Object>> grouped = group(records, groupField, interval);
        List<String> labels = new ArrayList<>(grouped.keySet());
        List<List<Double>> values = compute(request.metrics(), grouped, records);

        sort(request.metrics(), labels, values);

        Object data = request.chartType().equalsIgnoreCase("table")
                ? toTable(request, labels, values)
                : toChart(request, labels, values);

        return new WidgetResponse<>(200, "Widget fetch successfully", "success", data);
    }

    private Object emptyChart(Widget request) {
        // Chart with empty datasets and a default "all" label
        List<Dataset> datasets = request.metrics().stream()
                .map(m -> new Dataset(m.name(), List.of(BigDecimal.ZERO)))
                .toList();

        return new ChartData(
                request.chartType(),
                request.title(),
                List.of("all"),
                datasets);
    }

    private Object emptyTable(Widget request) {
        // Table with headers but no rows
        List<String> headers = new ArrayList<>();
        headers.add("Group"); // default first column
        request.metrics().forEach(m -> headers.add(m.name()));

        List<List<Object>> rows = List.of(); // empty rows

        return new TableData(
                "table",
                request.title(),
                headers,
                rows);
    }

    private ChartData toChart(Widget request, List<String> labels, List<List<Double>> values) {
        List<Dataset> datasets = new ArrayList<>();

        for (int i = 0; i < request.metrics().size(); i++) {
            var metric = request.metrics().get(i);
            List<BigDecimal> data = values.get(i).stream()
                    .map(BigDecimal::valueOf)
                    .toList();
            datasets.add(new Dataset(metric.name(), data));
        }

        return new ChartData(
                request.chartType(),
                request.title(),
                labels,
                datasets);
    }

    private TableData toTable(Widget request, List<String> labels, List<List<Double>> values) {
        List<String> headers = new ArrayList<>();
        headers.add("Group");
        request.metrics().forEach(m -> headers.add(m.name()));

        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            List<Object> row = new ArrayList<>();
            row.add(labels.get(i));
            for (List<Double> col : values) {
                row.add(BigDecimal.valueOf(col.get(i)));
            }
            rows.add(row);
        }

        return new TableData("table", request.title(), headers, rows);
    }

    private List<?> loadRecords(String schoolId, Widget request, int page, int size) {
        var dto = new ReportBuilderDTO(schoolId, request.entity(), request.filters());
        return switch (request.entity().toLowerCase()) {
            case "attendances" -> attendanceReportUseCase.execute(dto, page, size).getContent();
            case "students" -> studentReportUseCase.execute(dto, page, size).getContent();
            case "fees" -> feeReportUseCase.execute(dto, page, size).getContent();
            case "assessments" -> gradeReportUseCase.execute(dto, page, size).getContent();
            case "classes" -> classReportUseCase.execute(dto, page, size).getContent();
            case "parents" -> parentReportUseCase.execute(dto, page, size).getContent();
            case "payments" -> paymentReportUseCase.execute(dto, page, size).getContent();
            case "teachers" -> teacherReportUseCase.execute(dto, page, size).getContent();
            default -> {
                log.warn("Unknown entity: {}", request.entity());
                yield List.of();
            }
        };
    }

    private Map<String, List<Object>> group(List<?> records, String groupField, String interval) {
        if (groupField == null)
            return new LinkedHashMap<>(Map.of("all", new ArrayList<>(records)));

        return records.stream()
                .filter(r -> fieldValue(r, groupField) != null)
                .collect(Collectors.groupingBy(
                        r -> formatKey(fieldValue(r, groupField), interval),
                        TreeMap::new,
                        Collectors.mapping(r -> (Object) r, Collectors.toList())));
    }

    private List<List<Double>> compute(
            List<Metric> metrics,
            Map<String, List<Object>> grouped,
            List<?> all) {

        return metrics.stream()
                .map(m -> grouped.values().stream()
                        .map(g -> aggregate(m, filter(m, g), all))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private List<?> filter(Metric metric, List<?> records) {
        Map<String, String> tags = tags(metric);

        // metric-level filters take priority
        if (metric.filters() != null && !metric.filters().isEmpty()) {
            List<?> result = records;
            for (var f : metric.filters()) {
                result = result.stream()
                        .filter(r -> matches(fieldValue(r, f.field()), f.value()))
                        .collect(Collectors.toList());
            }
            return result;
        }

        // value tag filter
        if (tags.containsKey("value")) {
            String field = tags.getOrDefault("field", metric.field());
            String value = tags.get("value");
            return records.stream()
                    .filter(r -> matches(fieldValue(r, field), value))
                    .collect(Collectors.toList());
        }

        return records;
    }

    private double aggregate(Metric metric, List<?> filtered, List<?> all) {
        Map<String, String> tags = tags(metric);
        String field = metric.field();

        return switch (metric.aggregation().toLowerCase()) {
            case "sum" -> filtered.stream().mapToDouble(r -> numericField(r, field)).sum();

            case "avg" -> filtered.stream().mapToDouble(r -> numericField(r, field)).average().orElse(0.0);

            case "count" -> filtered.size();

            case "avgsum" -> {
                String subGroup = tags.getOrDefault("subGroupBy", "term");

                Map<String, List<Object>> subGrouped = filtered.stream()
                        .filter(r -> fieldValue(r, subGroup) != null)
                        .collect(Collectors.groupingBy(
                                r -> fieldValue(r, subGroup).toString(),
                                Collectors.mapping(r -> (Object) r, Collectors.toList())));

                if (subGrouped.isEmpty())
                    yield 0.0;

                double avg = subGrouped.values().stream()
                        .mapToDouble(g -> g.stream()
                                .mapToDouble(r -> numericField(r, field))
                                .sum())
                        .average()
                        .orElse(0.0);

                yield round(avg);
            }

            case "percentage" -> {
                String mode = tags.getOrDefault("mode", "count");
                double num = mode.equals("sum")
                        ? filtered.stream().mapToDouble(r -> numericField(r, field)).sum()
                        : filtered.size();
                double base = mode.equals("sum")
                        ? all.stream().mapToDouble(r -> numericField(r, field)).sum()
                        : all.size();
                if (tags.containsKey("normalizeBy"))
                    base = all.stream().mapToDouble(r -> numericField(r, tags.get("normalizeBy"))).max().orElse(base);
                yield base == 0 ? 0.0 : round(num * 100.0 / base);
            }

            default -> {
                log.warn("Unknown aggregation: {}", metric.aggregation());
                yield 0.0;
            }
        };
    }

    private void sort(List<Metric> metrics, List<String> labels, List<List<Double>> values) {
        for (int mi = 0; mi < metrics.size(); mi++) {
            Map<String, String> tags = tags(metrics.get(mi));
            if (!tags.containsKey("orderBy"))
                continue;

            String orderBy = tags.get("orderBy");
            boolean asc = tags.getOrDefault("order", "desc").equalsIgnoreCase("asc");

            // resolve sort key index — fallback to current metric
            int sortIdx = mi;
            for (int i = 0; i < metrics.size(); i++) {
                if (metrics.get(i).name().equalsIgnoreCase(orderBy)) {
                    sortIdx = i;
                    break;
                }
            }

            List<Double> key = values.get(sortIdx);
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < labels.size(); i++)
                idx.add(i);
            idx.sort((a, b) -> asc ? Double.compare(key.get(a), key.get(b)) : Double.compare(key.get(b), key.get(a)));

            List<String> sortedLabels = idx.stream().map(labels::get).collect(Collectors.toList());
            labels.clear();
            labels.addAll(sortedLabels);

            for (int vi = 0; vi < values.size(); vi++) {
                List<Double> orig = values.get(vi);
                List<Double> sorted = idx.stream().map(orig::get).collect(Collectors.toList());
                values.set(vi, sorted);
            }
            break;
        }
    }

    private String formatKey(Object value, String interval) {
        if (value == null)
            return "Unknown";

        LocalDate date = null;
        if (value instanceof LocalDate d)
            date = d;
        else if (value instanceof LocalDateTime dt)
            date = dt.toLocalDate();
        else if (value instanceof java.time.Instant i)
            date = i.atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        if (date != null)
            return switch (interval.toLowerCase()) {
                case "week" ->
                    date.getYear() + "-W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                case "month" -> date.getMonth().name();
                case "year" -> String.valueOf(date.getYear());
                default -> date.toString();
            };

        return value instanceof Enum<?> e ? e.name() : value.toString();
    }

    private double numericField(Object record, String fieldName) {
        Object val = fieldValue(record, fieldName);
        return val instanceof Number n ? n.doubleValue() : 0.0;
    }

    private Object fieldValue(Object record, String fieldName) {
        if (record == null || fieldName == null)
            return null;
        Class<?> clazz = record.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(record);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                log.warn("Cannot access field '{}' on {}", fieldName, record.getClass().getSimpleName());
                return null;
            }
        }
        return null;
    }

    private boolean matches(Object fieldVal, Object target) {
        return fieldVal != null && fieldVal.toString().equalsIgnoreCase(target.toString());
    }

    private String firstTag(Widget request, String key) {
        return firstTag(request, key, null);
    }

    private String firstTag(Widget request, String key, String fallback) {
        return request.metrics().stream()
                .map(m -> tags(m).get(key))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallback);
    }

    private Map<String, String> tags(Metric metric) {
        return metric.tags() != null ? metric.tags() : Map.of();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}