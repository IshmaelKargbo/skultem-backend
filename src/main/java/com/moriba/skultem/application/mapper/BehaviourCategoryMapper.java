package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.BehaviourCategoryDTO;
import com.moriba.skultem.domain.model.BehaviourCategory;

public class BehaviourCategoryMapper {
    public static BehaviourCategoryDTO toDTO(BehaviourCategory param) {
        return new BehaviourCategoryDTO(param.getId(), param.getName(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
