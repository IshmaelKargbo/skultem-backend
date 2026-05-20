package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.MaterialCategoryDTO;
import com.moriba.skultem.domain.model.MaterialCategory;

public class MaterialCategoryMapper {
    public static MaterialCategoryDTO toDTO(MaterialCategory param) {
        if (param == null)
            return null;
        return new MaterialCategoryDTO(param.getId(), param.getName(), param.getDescription(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
