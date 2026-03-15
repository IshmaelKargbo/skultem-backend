package com.moriba.skultem.infrastructure.persistence.specs;

import org.springframework.data.jpa.domain.Specification;
import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class TextSpecification {

    public static <T> Specification<T> build(String field, FilterOperator operator, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return null;

            String v = value.toLowerCase();
            Path<String> column = getPath(root, field);

            return switch (operator) {
                case EQUALS -> cb.equal(cb.lower(column), v);
                case NOT_EQUALS -> cb.notEqual(cb.lower(column), v);
                case STARTS_WITH -> cb.like(cb.lower(column), v + "%");
                case ENDS_WITH -> cb.like(cb.lower(column), "%" + v);
                case CONTAINS -> cb.like(cb.lower(column), "%" + v + "%");
                default -> null;
            };
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<String> getPath(Root<T> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        From<?, ?> current = root;
        Path<?> path = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            current = getOrCreateJoin(current, part);
        }

        path = current.get(parts[parts.length - 1]);
        return (Path<String>) path;
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {
        return from.getJoins().stream()
                .filter(j -> j.getAttribute().getName().equals(attribute))
                .findFirst()
                .orElseGet(() -> from.join(attribute, JoinType.LEFT));
    }
}