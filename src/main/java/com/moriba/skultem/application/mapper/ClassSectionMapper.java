package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.ClassSectionDTO;
import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Section;

public class ClassSectionMapper {
    public static ClassSectionDTO toDTO(ClassSection param, String name) {
        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        SectionDTO section = SectionMapper.toDTO(param.getSection());

        return new ClassSectionDTO(param.getId(), clazz, section, name, param.getCreatedAt(),
                param.getUpdatedAt());
    }

     public static ClassSectionDTO toDTO(Clazz param, Section s) {
        ClassDTO clazz = ClassMapper.toDTO(param);
        SectionDTO section = SectionMapper.toDTO(s);

        return new ClassSectionDTO(param.getId(), clazz, section, s.getName(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
