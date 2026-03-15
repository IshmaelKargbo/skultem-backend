package com.moriba.skultem.infrastructure.persistence.specs;

import org.springframework.data.jpa.domain.Specification;

import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.persistence.criteria.Expression;

public class NumberSpecification {

    public static <T> Specification<T> build(
            String field,
            FilterOperator operator,
            String value,
            String valueTo) {

        return (root, query, cb) -> {
            Expression<Number> column = PathResolver.getPath(root, field);

            Double v = value != null ? Double.valueOf(value) : null;
            Double v2 = valueTo != null ? Double.valueOf(valueTo) : null;

            return switch (operator) {

                case EQUALS -> cb.equal(column, v);

                case NOT_EQUALS -> cb.notEqual(column, v);

                case GREATER_THAN -> cb.gt(column, v);

                case GREATER_THAN_OR_EQUAL -> cb.ge(column, v);

                case LESS_THAN -> cb.lt(column, v);

                case LESS_THAN_OR_EQUAL -> cb.le(column, v);

                case BETWEEN -> cb.between(column.as(Double.class), v, v2);

                default -> null;
            };
        };
    }
}