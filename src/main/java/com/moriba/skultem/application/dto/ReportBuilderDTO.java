package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.Filter;

public record ReportBuilderDTO(String schoolId, String entity, List<Filter> filters) {
}
