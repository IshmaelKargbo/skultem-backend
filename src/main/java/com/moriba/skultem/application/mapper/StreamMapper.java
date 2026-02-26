package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.domain.model.Stream;

public class StreamMapper {
    public static StreamDTO toDTO(Stream param) {
        if (param == null)
            return null;

        return new StreamDTO(param.getId(), param.getName(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
