package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonMapper() {
    }

    // Convert Object -> JSON
    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    // Convert JSON -> Object
    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize JSON to " + type.getSimpleName(), e);
        }
    }

    // Convert JSON -> Generic type
    public static <T> T fromJson(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }

    // Convert JSON -> List<T>
    public static <T> List<T> fromJsonList(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return MAPPER.readValue(
                    json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize JSON to List<" + type.getSimpleName() + ">", e);
        }
    }
}