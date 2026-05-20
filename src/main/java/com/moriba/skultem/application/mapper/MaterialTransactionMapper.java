
package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.MaterialTrasactionDTO;
import com.moriba.skultem.domain.model.MaterialTransaction;

public class MaterialTransactionMapper {
    public static MaterialTrasactionDTO toDTO(MaterialTransaction param) {
        var material = MaterialMapper.toDTO(param.getMaterial());
        return new MaterialTrasactionDTO(param.getId(), material, param.getQty(), param.getNote(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
