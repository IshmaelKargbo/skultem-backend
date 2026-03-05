package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.domain.model.School;

public class SchoolMapper {
    public static SchoolDTO toDTO(School param) {
        return new SchoolDTO(param.getId(), param.getName(), param.getDomain(), param.getAddress(), param.getOwner(),
                param.getStatus(), param.getGradingScale(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
