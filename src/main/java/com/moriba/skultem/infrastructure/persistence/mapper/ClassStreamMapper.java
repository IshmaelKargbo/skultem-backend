package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassStream;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassStreamEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;

public class ClassStreamMapper {
    public static ClassStream toDomain(ClassStreamEntity param) {
        Stream stream = null;
        Clazz clazz = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        if (param.getStream() != null) {
            stream = StreamMapper.toDomain(param.getStream());
        }

        return new ClassStream(param.getId(), param.getSchoolId(), stream, clazz, param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static ClassStreamEntity toEntity(ClassStream args) {
        StreamEntity stream = null;
        ClassEntity clazz = null;

        if (args.getStream() != null) {
            clazz = ClassMapper.toEntity(args.getClazz());
        }

        if (args.getStream() != null) {
            stream = StreamMapper.toEntity(args.getStream());
        }

        return ClassStreamEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .stream(stream)
                .clazz(clazz)
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
