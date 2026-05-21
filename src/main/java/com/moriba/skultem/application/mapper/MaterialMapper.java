
package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.MaterialDTO;
import com.moriba.skultem.domain.model.Material;

public class MaterialMapper {
    public static MaterialDTO toDTO(Material param) {
        if (param == null) return null;
        var category = MaterialCategoryMapper.toDTO(param.getCategory());
    return new MaterialDTO(param.getId(), param.getName(), param.getUnit(), category, param.getStockQuantity(), param.getReorderLevel(), param.getLastRestockedAt(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
