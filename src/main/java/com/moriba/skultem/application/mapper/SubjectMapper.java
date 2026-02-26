package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.domain.model.Subject;

public class SubjectMapper {
    public static SubjectDTO toDTO(Subject param) {
        return new SubjectDTO(param.getId(), param.getName(), param.getCode(), param.getDescription(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
