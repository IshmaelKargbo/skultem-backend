package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.domain.model.SubjectGroup;

public class SubjectGroupMapper {
    public static SubjectGroupDTO toDTO(SubjectGroup param) {

        if (param == null) {
            return null;
        }

        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        StreamDTO stream = StreamMapper.toDTO(param.getStream());

        String streamName = "N/A", streamId = "";
        String className = "N/A", classId = "";

        if (stream != null) {
            streamName = stream.name();
            streamId = stream.id();
        }

        if (clazz != null) {
            className = clazz.name();
            classId = clazz.id();
        }

        return new SubjectGroupDTO(param.getId(), param.getName(), param.getLevel().toString(), className,
                classId, streamName, streamId, param.getRequired(), param.getMinSelection(), param.getMaxSelection(),
                param.getDisplayOrder(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
