
package com.moriba.skultem.application.dto;

public record WidgetResponse<T>(
        int code,
        String message,
        String status,
        T data
) {}
