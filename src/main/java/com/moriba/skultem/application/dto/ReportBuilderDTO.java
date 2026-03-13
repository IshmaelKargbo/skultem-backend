package com.moriba.skultem.application.dto;

import java.util.List;

public record ReportBuilderDTO(String schoolId, String entity, List<FilterDTO> filters) {
}
