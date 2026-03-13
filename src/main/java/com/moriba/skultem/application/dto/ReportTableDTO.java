package com.moriba.skultem.application.dto;

import java.util.List;

public record ReportTableDTO(List<String> headers, List<List<String>> rows) {
}
