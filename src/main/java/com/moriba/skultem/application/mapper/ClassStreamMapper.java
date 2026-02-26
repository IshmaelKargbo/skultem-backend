package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.ClassStreamDTO;
import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.domain.model.ClassStream;

public class ClassStreamMapper {
    public static ClassStreamDTO toDTO(ClassStream param) {
        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        StreamDTO stream = StreamMapper.toDTO(param.getStream());

        return new ClassStreamDTO(param.getId(), clazz, stream, param.getCreatedAt(), param.getUpdatedAt());
    }
}
