package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.PayrollRunDTO;
import com.moriba.skultem.domain.model.PayrollRun;

public class PayrollRunMapper {
    public static PayrollRunDTO toDTO(PayrollRun param) {
        if (param == null) {
            return null;
        }

        return new PayrollRunDTO(
                param.getId(),
                param.getSchoolId(),
                param.getPeriod(),
                param.getPayDate(),
                param.getStatus(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
