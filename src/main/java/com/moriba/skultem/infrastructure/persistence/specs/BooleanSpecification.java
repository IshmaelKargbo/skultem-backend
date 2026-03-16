package com.moriba.skultem.infrastructure.persistence.specs;

import org.springframework.data.jpa.domain.Specification;
import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class BooleanSpecification {

    public static <T> Specification<T> build(String field, FilterOperator operator, Boolean value) {
        return (root, query, cb) -> {
            if (value == null) return null;

            Path<Boolean> column = getPath(root, field);

            return switch (operator) {
                case EQUALS -> cb.equal(column, value);
                case NOT_EQUALS -> cb.notEqual(column, value);
                default -> null;
            };
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<Boolean> getPath(Root<T> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        From<?, ?> current = root;
        Path<?> path = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            current = getOrCreateJoin(current, part);
        }

        path = current.get(parts[parts.length - 1]);
        return (Path<Boolean>) path;
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {
        return from.getJoins().stream()
                .filter(j -> j.getAttribute().getName().equals(attribute))
                .findFirst()
                .orElseGet(() -> from.join(attribute, JoinType.LEFT));
    }
}