package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;
import java.util.Map;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private String status;
    private int code;
    private String message;
    private T data;
    private Map<String, Object> meta;
    private Instant timestamp;

    public ApiResponse(String status, int code, String message, T data, Map<String, Object> meta, Instant timestamp) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.meta = meta;
        this.timestamp = timestamp;
    }

    public ApiResponse(String status, int code, String message, T data, Map<String, Object> meta) {
        this(status, code, message, data, meta, Instant.now());
    }

    public ApiResponse(String status, int code, String message, T data) {
        this(status, code, message, data, null, Instant.now());
    }

    public ApiResponse(int code, String message, T data) {
        this("error", code, message, data, null, Instant.now());
    }

    public ApiResponse(int code, String message) {
        this("error", code, message, null, null, Instant.now());
    }
}