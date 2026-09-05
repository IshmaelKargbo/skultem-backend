package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.moriba.skultem.domain.model.SalaryTemplate;
import com.moriba.skultem.domain.vo.PayComponent;
import com.moriba.skultem.infrastructure.persistence.entity.PayComponentEmbeddable;
import com.moriba.skultem.infrastructure.persistence.entity.SalaryTemplateEntity;

public class SalaryTemplateMapper {
    public static SalaryTemplate toDomain(SalaryTemplateEntity param) {
        if (param == null) {
            return null;
        }

        return new SalaryTemplate(
                param.getId(),
                param.getSchoolId(),
                param.getName(),
                param.getBasicSalary(),
                toDomainItems(param.getAllowances()),
                toDomainItems(param.getDeductions()),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static SalaryTemplateEntity toEntity(SalaryTemplate param) {
        if (param == null) {
            return null;
        }

        return SalaryTemplateEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .basicSalary(param.getBasicSalary())
                .allowances(toEntityItems(param.getAllowances()))
                .deductions(toEntityItems(param.getDeductions()))
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }

    private static List<PayComponent> toDomainItems(List<PayComponentEmbeddable> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> new PayComponent(item.getName(), item.getType(), item.getValue()))
                .toList();
    }

    private static List<PayComponentEmbeddable> toEntityItems(List<PayComponent> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> PayComponentEmbeddable.builder()
                        .name(item.name())
                        .type(item.type())
                        .value(item.value())
                        .build())
                .toList();
    }
}
