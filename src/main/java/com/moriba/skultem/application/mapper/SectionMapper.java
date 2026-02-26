package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.domain.model.Section;

public class SectionMapper {
    public static SectionDTO toDTO(Section param) {
        return new SectionDTO(param.getId(), param.getName(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
