package com.moriba.skultem.application.dto;

import java.util.List;

public record TableData(
        String tableType,
        String title,
        List<String> headers,
        List<List<Object>> rows
) {}
