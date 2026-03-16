package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.moriba.skultem.domain.model.SaveReport;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.SaveReportEntity;

public final class SaveReportMapper {

    private SaveReportMapper() {
    }

    public static SaveReport toDomain(SaveReportEntity entity) {
        if (entity == null) {
            return null;
        }

        List<Filter> filters = JsonMapper.fromJsonList(entity.getFilters(), Filter.class);

        return new SaveReport(
                entity.getId(),
                entity.getSchoolId(),
                entity.getName(),
                filters,
                entity.getEntity(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static SaveReportEntity toEntity(SaveReport domain) {
        if (domain == null) {
            return null;
        }

        return SaveReportEntity.builder()
                .id(domain.getId())
                .schoolId(domain.getSchoolId())
                .name(domain.getName())
                .entity(domain.getEntity())
                .filters(JsonMapper.toJson(domain.getFilters()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}