package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.RequestDemoDTO;
import com.moriba.skultem.domain.model.RequestDemo;

public class RequestDemoMapper {
    public static RequestDemoDTO toDTO(RequestDemo param) {
        return new RequestDemoDTO(param.getId(), param.getName(), param.getEmail(), param.getSchool(), param.getPhone(),
                param.getPreferred(), param.getPriority(), param.getMessage(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
