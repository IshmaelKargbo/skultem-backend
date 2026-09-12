package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.domain.model.Supply;

public class SupplyMapper {
    public static SupplyDTO toDTO(Supply param) {
        var student = param.getStudent() != null ? StudentMapper.toDTO(param.getStudent(), null) : null;
        var material = MaterialMapper.toDTO(param.getMaterial());

        return new SupplyDTO(param.getId(), student, param.getCustomerName(), material, param.getQty(),
                param.getCollectedQty(), param.getCollectedOn(), param.getStatus(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
