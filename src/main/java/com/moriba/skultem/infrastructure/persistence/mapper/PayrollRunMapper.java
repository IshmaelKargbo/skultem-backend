package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.PayrollRun;
import com.moriba.skultem.infrastructure.persistence.entity.PayrollRunEntity;

public class PayrollRunMapper {
    public static PayrollRun toDomain(PayrollRunEntity param) {
        if (param == null) {
            return null;
        }

        return new PayrollRun(
                param.getId(),
                param.getSchoolId(),
                param.getPeriod(),
                param.getPayDate(),
                param.getStatus(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static PayrollRunEntity toEntity(PayrollRun param) {
        if (param == null) {
            return null;
        }

        return PayrollRunEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .period(param.getPeriod())
                .payDate(param.getPayDate())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
