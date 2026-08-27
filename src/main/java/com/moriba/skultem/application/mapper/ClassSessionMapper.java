package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassSessionRecord;
import com.moriba.skultem.domain.model.ClassSession;

public class ClassSessionMapper {
    public static ClassSessionRecord toDTO(ClassSession param) {
        String streamName = "";
        String streamId = "";

        if (param.getStream() != null) {
            streamName = param.getStream().getName();
            streamId = param.getStream().getId();
        }

        return new ClassSessionRecord(param.getId(), streamName, streamId, param.getSection().getName(),
                param.getSection().getId());
    }
}
