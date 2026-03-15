package com.moriba.skultem.infrastructure.persistence.specs;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

public class PathResolver {
    @SuppressWarnings("unchecked")
    public static <T, V> Path<V> getPath(Root<T> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Path<?> path = root;

        for (int i = 0; i < parts.length - 1; i++) {
            path = ((From<?, ?>) path).join(parts[i], JoinType.LEFT);
        }

        return (Path<V>) path.get(parts[parts.length - 1]);
    }
}
