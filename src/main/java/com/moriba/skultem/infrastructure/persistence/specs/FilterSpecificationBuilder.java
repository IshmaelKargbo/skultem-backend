package com.moriba.skultem.infrastructure.persistence.specs;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.moriba.skultem.domain.vo.Filter;

public class FilterSpecificationBuilder {

    public static <T> Specification<T> build(List<Filter> filters) {

        Specification<T> spec = (root, query, cb) -> cb.conjunction();

        if (filters == null || filters.isEmpty()) {
            return spec;
        }

        for (Filter filter : filters) {

            Specification<T> filterSpec = buildFilter(filter);

            if (filterSpec != null) {
                spec = spec.and(filterSpec);
            }
        }

        return spec;
    }

    private static <T> Specification<T> buildFilter(Filter filter) {
        String field = mapToJavaField(filter.field());

        return switch (filter.type()) {
            case "text" -> TextSpecification.build(
                    field,
                    filter.operator(),
                    filter.value());

            case "select" -> SelectSpecification.build(
                    field,
                    filter.operator(),
                    filter.value(),
                    filter.values());

            case "boolean" -> BooleanSpecification.build(
                    field,
                    filter.operator(),
                    parseBoolean(filter.value()));

            case "number" -> NumberSpecification.build(
                    field,
                    filter.operator(),
                    filter.value(),
                    filter.valueTo());

            case "date", "instant", "age" -> DateSpecification.build(filter);

            default -> null;
        };
    }

    private static String mapToJavaField(String field) {
        String[] parts = field.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = snakeToCamel(parts[i]);
        }
        return String.join(".", parts);
    }

    private static String snakeToCamel(String str) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char c : str.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else {
                result.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            }
        }
        return result.toString();
    }

    private static Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }
}