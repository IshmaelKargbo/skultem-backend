package com.moriba.skultem.application.dto;

import lombok.Getter;

@Getter
public class ReportResponse<T> {
    private T data;
    private Object meta;

    public ReportResponse(T data, Object meta) {
        this.data = data;
        this.meta = meta;
    }
}
