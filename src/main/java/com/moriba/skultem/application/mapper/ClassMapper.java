package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.domain.model.Clazz;

public class ClassMapper {
    public static ClassDTO toDTO(Clazz param) {
        if (param == null) {
            return null;
        }

        ClassDTO nextClass = null;

        if (param.getNextClass() != null) {
            nextClass = toDTO(param.getNextClass());
        }

        var template = param.getTemplate();
        return new ClassDTO(param.getId(), param.getName(), param.getDisplayOrder(), nextClass,
                param.getLevel(),
                template != null ? template.getId() : null,
                template != null ? template.getName() : null,
                param.getTerminal(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
