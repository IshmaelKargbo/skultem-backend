package com.moriba.skultem.infrastructure.persistence.specs;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.persistence.criteria.Path;

public class SelectSpecification {

    public static <T> Specification<T> build(
            String field,
            FilterOperator operator,
            Object value,
            List<?> values) {

        return (root, query, cb) -> {
            Path<Object> column = PathResolver.getPath(root, field);

            return switch (operator) {
                case EQUALS -> {
                    if (value == null) {
                        yield cb.isNull(column);
                    }
                    yield cb.equal(column, value);
                }
                case NOT_EQUALS -> {
                    if (value == null) {
                        yield cb.isNotNull(column);
                    }
                    yield cb.notEqual(column, value);
                }
                case IN_LIST -> column.in(values);
                default -> null;
            };
        };
    }
}