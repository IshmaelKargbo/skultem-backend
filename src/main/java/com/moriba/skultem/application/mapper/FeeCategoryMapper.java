package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.domain.model.FeeCategory;

public class FeeCategoryMapper {
    public static FeeCategoryDTO toDTO(FeeCategory param) {
        return new FeeCategoryDTO(param.getId(), param.getName(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
