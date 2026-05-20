
package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.domain.model.Supply;

public class SupplyMapper {
    public static SupplyDTO toDTO(Supply param) {
        var student = StudentMapper.toDTO(param.getStudent(), null);
        var material = MaterialMapper.toDTO(param.getMaterial());

        return new SupplyDTO(param.getId(), student, material, param.getQty(), param.getCollectedQty(),
                param.getCollectedOn(), param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
