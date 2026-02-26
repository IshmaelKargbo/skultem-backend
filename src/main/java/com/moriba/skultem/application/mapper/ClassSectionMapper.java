package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.ClassSectionDTO;
import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.domain.model.ClassSection;

public class ClassSectionMapper {
    public static ClassSectionDTO toDTO(ClassSection param, String name) {
        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        SectionDTO section = SectionMapper.toDTO(param.getSection());

        return new ClassSectionDTO(param.getId(), clazz, section, name, param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
