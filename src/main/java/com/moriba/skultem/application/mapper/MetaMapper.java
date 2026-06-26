package com.moriba.skultem.application.mapper;

import java.util.Map;

import org.springframework.data.domain.Page;

public class MetaMapper {
    public static Map<String, Object> toMeta(Page<?> page) {
        return Map.of(
                "page", page.getNumber() + 1,
                "size", page.getSize(),
                "count", page.getTotalElements(),
                "pages", page.getTotalPages());
    }
}